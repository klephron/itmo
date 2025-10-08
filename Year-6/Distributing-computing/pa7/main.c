#define _GNU_SOURCE

#include "argparse.h"
#include "banking.h"
#include "common.h"
#include "ipc.h"
#include "pa2345.h"

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

typedef enum status_t {
  STATUS_OK = 0,
  STATUS_BAD_ALLOC = 1,
  STATUS_BAD_PIPE = 2,
  STATUS_BAD_FILE = 3,
  STATUS_BAD_FORK = 4,
  STATUS_BAD_USAGE = 255,
  STATUS_EOF = 10,
  STATUS_EAGAIN = 11,
} status;

typedef struct args_t {
  int proc_n;
  const char *pipes_path;
  const char *events_path;
  int *balances;
} args;

typedef struct ctx_pipe_t {
  int r;
  int w;
} ctx_pipe;

typedef enum ctx_pipe_type_t {
  PIPE_UNDEFINED = 0,
  PIPE_UNUSED,
  PIPE_READER,
  PIPE_WRITER,
} ctx_pipe_type;

typedef int local_time_t;

typedef struct ctx_t {
  const args *args;

  FILE *events_f;
  FILE *pipes_f;

  ctx_pipe *pipes;

  int proc_id;

  balance_t balance;

  struct {
    local_time_t *vec;
  } clock;
} ctx;

typedef struct ctx_pipe_idx_t {
  int i;
  int j;
} ctx_pipe_idx;

typedef struct rec_ctx_t {
  ctx *ctx;
  int nio; // nonblocking
} rec_ctx;

__attribute__((unused)) static const char *msg_type_str(MessageType type) {
  switch (type) {
  case STARTED:
    return "STARTED";
  case DONE:
    return "DONE";
  case ACK:
    return "ACK";
  case STOP:
    return "STOP";
  case TRANSFER:
    return "TRANSFER";
  case BALANCE_HISTORY:
    return "BALANCE_HISTORY";
  case CS_REQUEST:
    return "CS_REQUEST";
  case CS_REPLY:
    return "CS_REPLY";
  case CS_RELEASE:
    return "CS_RELEASE";
  case BALANCE_STATE:
    return "BALANCE_STATE";
  case SNAPSHOT_VTIME:
    return "SNAPSHOT_VTIME";
  case SNAPSHOT_ACK:
    return "SNAPSHOT_ACK";
  case EMPTY:
    return "EMPTY";
  }
  return "UNKNOWN";
}

static void args_deinit(args *args) {
  if (args->balances) {
    free(args->balances);
    args->balances = NULL;
  }
}

static int args_parse(args *args, int argc, const char **argv) {
  static const char *const s_usages[] = {
      "app [options] balances...\n"
      "    balances...     list of integer process balances, length\n"
      "                    must be equal to number of processes",
      NULL,
  };

  struct argparse_option options[] = {
      OPT_HELP(),
      OPT_GROUP("Options:"),
      OPT_INTEGER('p', "procs", &args->proc_n, "number of processes"),
      OPT_END(),
  };

  struct argparse argparse;
  argparse_init(&argparse, options, s_usages, ARGPARSE_STOP_AT_NON_OPTION);
  argparse_describe(&argparse, NULL, NULL);

  argc = argparse_parse(&argparse, argc, argv);

  if (argc != args->proc_n) {
    argparse_usage(&argparse);
    return STATUS_BAD_USAGE;
  }

  args->balances = (int *)malloc(sizeof(int) * args->proc_n);
  if (!args->balances) {
    return STATUS_BAD_ALLOC;
  }

  for (int i = 0; i < args->proc_n; ++i) {
    char *endptr;
    long balance = strtol(argv[i], &endptr, 10);

    if (*endptr != '\0' || balance > INT_MAX || balance < INT_MIN) {
      args_deinit(args);

      argparse_usage(&argparse);
      return STATUS_BAD_USAGE;
    }

    args->balances[i] = balance;
  }

  return STATUS_OK;
}

static void logs_close(ctx *ctx) {
  if (ctx->events_f) {
    fclose(ctx->events_f);
    ctx->events_f = NULL;
  }
  if (ctx->pipes_f) {
    fclose(ctx->pipes_f);
    ctx->events_f = NULL;
  }
}

static int logs_open(ctx *ctx) {
  int status = 0;

  if (!(ctx->events_f = fopen(ctx->args->events_path, "w"))) {
    status = STATUS_BAD_FILE;
  }
  if (!(ctx->pipes_f = fopen(ctx->args->pipes_path, "w"))) {
    status = STATUS_BAD_FILE;
  }

  if (status) {
    logs_close(ctx);
  }

  return status;
}

static void logs_event_write(ctx *ctx, char *data, int size) {
  fwrite(data, sizeof(*data), size, stdout);
  fwrite(data, sizeof(*data), size, ctx->events_f);
}

static int pipes_n(const ctx *ctx) {
  return ctx->args->proc_n + 1;
}

static ctx_pipe *pipes_select(const ctx *ctx, ctx_pipe_idx idx) {
  return ctx->pipes + ((idx.i * pipes_n(ctx)) + idx.j);
}

static ctx_pipe_idx pipes_idx(const ctx *ctx, ctx_pipe *p) {
  const int n = pipes_n(ctx);

  int idx = p - ctx->pipes;
  int i = idx / n;
  int j = idx % n;
  return (ctx_pipe_idx){.i = i, .j = j};
}

static void pipes_close1(ctx_pipe *p) {
  close(p->r);
  close(p->w);
}

static ctx_pipe_type pipes_type(ctx *ctx, ctx_pipe_idx idx) {
  if (idx.i == idx.j) {
    return PIPE_UNDEFINED;
  }
  if (ctx->proc_id == idx.i) {
    return PIPE_WRITER;
  }
  if (ctx->proc_id == idx.j) {
    return PIPE_READER;
  }
  return PIPE_UNUSED;
}

static ctx_pipe_type pipes_type_pipe(ctx *ctx, ctx_pipe *p) {
  return pipes_type(ctx, pipes_idx(ctx, p));
}

static void pipes_close(ctx *ctx) {
  const int n = pipes_n(ctx);

  if (ctx->pipes) {
    for (int i = 0; i < n; ++i) {
      for (int j = 0; j < n; ++j) {
        ctx_pipe_idx idx = {.i = i, .j = j};

        if (pipes_type(ctx, idx) == PIPE_UNDEFINED) {
          continue;
        }

        ctx_pipe *p = pipes_select(ctx, idx);
        pipes_close1(p);
      }
    }
    free(ctx->pipes);
    ctx->pipes = NULL;
  }
}

static int pipes_open(ctx *ctx) {
  const int n = pipes_n(ctx);

  ctx->pipes = calloc(1, sizeof(*ctx->pipes) * n * n);

  if (!ctx->pipes) {
    return STATUS_BAD_ALLOC;
  }

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      ctx_pipe_idx idx = {.i = i, .j = j};

      if (pipes_type(ctx, idx) == PIPE_UNDEFINED) {
        continue;
      }

      ctx_pipe *p = pipes_select(ctx, idx);
      if (pipe2((int *)p, O_NONBLOCK)) {
        goto err;
      }
      fprintf(ctx->pipes_f, "%d/%d\n", p->r, p->w);
    }
  }

  fflush(ctx->pipes_f);
  return 0;

err:
  fflush(ctx->pipes_f);
  pipes_close(ctx);
  return STATUS_BAD_PIPE;
}

// closes and configures all pipes
static void pipes_configure(ctx *ctx) {
  const int n = pipes_n(ctx);

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      ctx_pipe_idx idx = (ctx_pipe_idx){.i = i, .j = j};
      ctx_pipe *p = pipes_select(ctx, idx);

      switch (pipes_type(ctx, idx)) {
      case PIPE_UNDEFINED: {
        break;
      }
      case PIPE_READER: {
        close(p->w);
        break;
      }
      case PIPE_WRITER: {
        close(p->r);
        break;
      }
      case PIPE_UNUSED: {
        close(p->r);
        close(p->w);
        break;
      }
      }
    }
  }
}

// match proc_id with idx cuz they may be encoded differently
static ctx_pipe *pipes_select_proc_id(ctx *ctx, int id_from, int id_to) {
  int i = id_from;
  int j = id_to;
  return pipes_select(ctx, (ctx_pipe_idx){.i = i, .j = j});
}

static int pipes_writer_fd(ctx *ctx, int id_to) {
  ctx_pipe *p = pipes_select_proc_id(ctx, ctx->proc_id, id_to);

  assert(pipes_type_pipe(ctx, p) == PIPE_WRITER);

  return p->w;
}

static int pipes_reader_fd(ctx *ctx, int id_from) {
  ctx_pipe *p = pipes_select_proc_id(ctx, id_from, ctx->proc_id);

  assert(pipes_type_pipe(ctx, p) == PIPE_READER);

  return p->r;
}

static int clock_n(const ctx *ctx) {
  return ctx->args->proc_n;
}

static void clock_deinit(ctx *ctx) {
  if (ctx->clock.vec) {
    free(ctx->clock.vec);
    ctx->clock.vec = NULL;
  }
}

static int clock_init(ctx *ctx) {
  const int n = clock_n(ctx);

  ctx->clock.vec = (local_time_t *)malloc(sizeof(local_time_t) * n);
  if (!ctx->clock.vec) {
    goto err;
  }

  for (int i = 0; i < n; ++i) {
    ctx->clock.vec[i] = 0;
  }

  return 0;

err:
  clock_deinit(ctx);
  return STATUS_BAD_ALLOC;
}

static int parent_is(ctx *ctx) {
  return ctx->proc_id == 0;
}

static int parent_fn(ctx *ctx) {
  int n = ctx->args->proc_n;

  Message message = {{0}};

  pipes_configure(ctx);

  for (int i = 1; i <= n; ++i) {
    int s = receive(&(rec_ctx){.ctx = ctx, .nio = 0}, i, &message);
    MessageType type = message.s_header.s_type;
    if (s || type != STARTED) {
      fprintf(stderr, "error: parent unable to receive_any %s, expected %s\n",
              msg_type_str(type), msg_type_str(STARTED));
    }
  }

  for (int i = 1; i <= n; ++i) {
    int s = receive(&(rec_ctx){.ctx = ctx, .nio = 0}, i, &message);
    MessageType type = message.s_header.s_type;
    if (s || message.s_header.s_type != DONE) {
      fprintf(stderr, "error: parent unable to receive_any %s, expected %s\n",
              msg_type_str(type), msg_type_str(DONE));
    }
  }

  int wpid;
  int status = 0;

  while ((wpid = wait(&status)) > 0)
    ;

  return 0;
}

static int child_fn(ctx *ctx) {
  __attribute__((unused)) static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 100000000,
  };

  const int n = ctx->args->proc_n;
  const int proc_id = ctx->proc_id;
  int status = 0;

  Message request = {
      .s_header =
          {
              .s_magic = MESSAGE_MAGIC,
              .s_payload_len = MAX_PAYLOAD_LEN,
          },
      .s_payload = {0},
  };
  Message response = {{0}};

  pipes_configure(ctx);

  // send started
  int payload_len = sprintf(request.s_payload, log_started_fmt, 0, proc_id,
                            getpid(), getppid(), ctx->balance);
  request.s_header.s_type = STARTED;
  request.s_header.s_payload_len = payload_len;

  if (send_multicast(ctx, &request)) {
    fprintf(stderr, "error: %i unable to send_multicast STARTED\n",
            ctx->proc_id);
  }

  logs_event_write(ctx, request.s_payload, payload_len);

  // receive started from all procs
  for (int i = 1; i <= n; ++i) {
    if (i == proc_id) {
      continue;
    }

    int s = receive(&(rec_ctx){.ctx = ctx, .nio = 0}, i, &response);

    if (s || response.s_header.s_type != STARTED) {
      fprintf(stderr, "error: %i unable to receive STARTED from id=%d\n",
              ctx->proc_id, i);
    }
  }

  // log received all started
  payload_len =
      sprintf(request.s_payload, log_received_all_started_fmt, 0, proc_id);

  logs_event_write(ctx, request.s_payload, payload_len);

  // send done
  payload_len =
      sprintf(request.s_payload, log_done_fmt, 0, proc_id, ctx->balance);
  request.s_header.s_type = DONE;
  request.s_header.s_payload_len = payload_len;

  if (send_multicast(ctx, &request)) {
    fprintf(stderr, "error: %i unable to send_multicast DONE\n", ctx->proc_id);
  }

  logs_event_write(ctx, request.s_payload, payload_len);

  // receive done from all procs
  for (int i = 1; i <= n; ++i) {
    if (i == proc_id) {
      continue;
    }

    int s;
    do {
      s = receive(&(rec_ctx){.ctx = ctx, .nio = 0}, i, &response);
      if (s) {
        fprintf(stderr, "error: %i unable to receive DONE from id=%d\n",
                ctx->proc_id, i);
        break;
      }
    } while (response.s_header.s_type != DONE);
  }

  // log received all done
  payload_len =
      sprintf(request.s_payload, log_received_all_done_fmt, 0, proc_id);

  logs_event_write(ctx, request.s_payload, payload_len);

  return status;
}

static void child_configure(ctx *ctx, int proc_id, balance_t balance) {
  ctx->proc_id = proc_id;
  ctx->balance = balance;
}

static int child_run_all(ctx *ctx) {
  for (int id = 1; id <= ctx->args->proc_n; ++id) {
    int pid = fork();

    if (pid == 0) {
      child_configure(ctx, id, ctx->args->balances[id - 1]);
      return child_fn(ctx);
    } else if (pid < 0) {
      return STATUS_BAD_FORK;
    }
  }
  return 0;
}

int send(void *self, local_id dst, const Message *msg) {
  ctx *ctx = self;

  int wfd = pipes_writer_fd(ctx, dst);
  int s = write(wfd, msg, msg->s_header.s_payload_len + sizeof(MessageHeader));

  return s == -1 ? -1 : STATUS_OK;
}

int send_multicast(void *self, const Message *msg) {
  ctx *ctx = self;
  int n = ctx->args->proc_n;

  int status = STATUS_OK;

  // to children and parent
  for (int id_to = 0; id_to <= n; ++id_to) {
    if (ctx->proc_id == id_to) {
      continue;
    }
    if (send(ctx, id_to, msg) == -1) {
      status = -1;
    }
  }

  return status;
}

int receive(void *self, local_id from, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  rec_ctx *rec = self;
  ctx *ctx = rec->ctx;
  int rfd = pipes_reader_fd(ctx, from);

  memset(msg->s_payload, 0, msg->s_header.s_payload_len);

  while (1) {
    int s = read(rfd, msg, sizeof(MessageHeader));

    if (s == -1) {
      if (rec->nio && errno == EAGAIN) {
        return STATUS_EAGAIN;
      }
      nanosleep(&s_ts, NULL);
      continue;
    }
    if (s == 0) {
      return STATUS_EOF;
    }

    if (msg->s_header.s_payload_len) {
      s = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
      if (s <= 0) {
        return -1;
      }
    }
    return STATUS_OK;
  }
}

int receive_any(void *self, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  rec_ctx *rec = self;
  ctx *ctx = rec->ctx;
  const int n = ctx->args->proc_n;
  const int n_eofs = ctx->proc_id == 0 ? n : n - 1;

  memset(msg->s_payload, 0, msg->s_header.s_payload_len);

  for (int id_from = 1, eofs = 0; eofs < n_eofs; ++id_from) {
    // sleep and loop again
    if (id_from > n) {
      if (rec->nio) {
        return STATUS_EAGAIN;
      }
      id_from = 1;
      eofs = 0;
      nanosleep(&s_ts, NULL);
    }

    if (id_from == ctx->proc_id) {
      continue;
    }

    int rfd = pipes_reader_fd(ctx, id_from);
    int s = read(rfd, msg, sizeof(MessageHeader));

    if (s == 0) {
      ++eofs;
      continue;
    }
    if (s == -1) {
      continue;
    }

    if (msg->s_header.s_payload_len) {
      s = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
      if (s <= 0) {
        return -1;
      }
    }
    return STATUS_OK;
  }
  return STATUS_EOF;
}

void transfer(void *parent_data, local_id src, local_id dst, balance_t amount) {
}

int main(int argc, const char **argv) {
  int status;

  args args = {
      .proc_n = 0,
      .events_path = events_log,
      .pipes_path = pipes_log,
      .balances = NULL,
  };
  ctx ctx = {
      .args = &args,
      .events_f = NULL,
      .pipes_f = NULL,
      .pipes = NULL,
      .proc_id = 0, // parent
      .balance = 0,
      .clock = {0},
  };

  if ((status = args_parse(&args, argc, argv))) {
    return status;
  }

  if ((status = logs_open(&ctx))) {
    goto cleanup;
  }

  if ((status = pipes_open(&ctx))) {
    goto cleanup;
  }

  if ((status = child_run_all(&ctx))) {
    goto cleanup;
  }

  if (parent_is(&ctx)) {
    status = parent_fn(&ctx);
  }

cleanup:
  pipes_close(&ctx);

  logs_close(&ctx);

  args_deinit(&args);

  return status;
}
