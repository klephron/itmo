#define _GNU_SOURCE

#include "argparse.h"
#include "common.h"
#include "ipc.h"
#include "pa2345.h"

#include <assert.h>
#include <fcntl.h>
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
} status;

typedef struct args_t {
  int proc_n;
  int mutexl;
  const char *pipes_path;
  const char *events_path;
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

typedef struct ctx_t {
  const args *args;

  FILE *events_f;
  FILE *pipes_f;

  ctx_pipe *pipes;

  int proc_id;
} ctx;

typedef struct ctx_pipe_idx_t {
  int i;
  int j;
} ctx_pipe_idx;

static int args_parse(args *args, int argc, const char **argv) {
  static const char *const s_usages[] = {
      "app [options]",
      NULL,
  };

  struct argparse_option options[] = {
      OPT_GROUP("Options:"),
      OPT_HELP(),
      OPT_INTEGER('p', "processes", &args->proc_n, "number of processes"),
      OPT_BOOLEAN('l', "mutexl", &args->mutexl, "with mutex lock"),
      OPT_END(),
  };

  struct argparse argparse;
  argparse_init(&argparse, options, s_usages, ARGPARSE_STOP_AT_NON_OPTION);
  argparse_describe(&argparse, NULL, NULL);

  int status = argparse_parse(&argparse, argc, argv);

  if (status) {
    argparse_usage(&argparse);
    return STATUS_BAD_USAGE;
  }
  return status;
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
  };
  if (!(ctx->pipes_f = fopen(ctx->args->pipes_path, "w"))) {
    status = STATUS_BAD_FILE;
  };

  if (status) {
    logs_close(ctx);
  }

  return status;
}

static void logs_event_write(ctx *ctx, char *data, int size) {
  fwrite(data, sizeof(*data), size, stdout);
  fwrite(data, sizeof(*data), size, ctx->events_f);
}

static int pipes_proc_n(const ctx *ctx) {
  return ctx->args->proc_n + 1;
}

static ctx_pipe *pipes_select(const ctx *ctx, ctx_pipe_idx idx) {
  return ctx->pipes + ((idx.i * pipes_proc_n(ctx)) + idx.j);
}

static ctx_pipe_idx pipes_idx(const ctx *ctx, ctx_pipe *p) {
  const int n = pipes_proc_n(ctx);

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
  const int n = pipes_proc_n(ctx);

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
  const int n = pipes_proc_n(ctx);

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
        goto error;
      }
      fprintf(ctx->pipes_f, "%d/%d\n", p->r, p->w);
    }
  }

  fflush(ctx->pipes_f);
  return 0;

error:
  fflush(ctx->pipes_f);
  pipes_close(ctx);
  return STATUS_BAD_PIPE;
}

// closes and configures all pipes
static void pipes_configure(ctx *ctx) {
  const int n = pipes_proc_n(ctx);

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
static ctx_pipe *pipes_select_proc(ctx *ctx, int id_from, int id_to) {
  int i = id_from;
  int j = id_to;
  return pipes_select(ctx, (ctx_pipe_idx){.i = i, .j = j});
}

static int pipes_writer_fd(ctx *ctx, int id_to) {
  ctx_pipe *p = pipes_select_proc(ctx, ctx->proc_id, id_to);

  assert(pipes_type_pipe(ctx, p) == PIPE_WRITER);

  return p->w;
}

static int pipes_reader_fd(ctx *ctx, int id_from) {
  ctx_pipe *p = pipes_select_proc(ctx, id_from, ctx->proc_id);

  assert(pipes_type_pipe(ctx, p) == PIPE_READER);

  return p->r;
}

static int parent_is(ctx *ctx) {
  return ctx->proc_id == 0;
}

static int parent_fn(ctx *ctx) {
  int n = ctx->args->proc_n;

  Message message = {{0}};

  pipes_configure(ctx);

  // receive started
  for (int i = 0; i < n; ++i) {
    int s = receive_any(ctx, &message);
    if (s) {
      fprintf(stderr, "error: parent unable to receive_any\n");
    }
  }

  // receive done
  for (int i = 0; i < n; ++i) {
    int s = receive_any(ctx, &message);
    if (s) {
      fprintf(stderr, "error: parent unable to receive_any\n");
    }
  }

  int wpid;
  int status = 0;

  while ((wpid = wait(&status)) > 0)
    ;

  return 0;
}

static int proc_fn(ctx *ctx) {
  const int n = ctx->args->proc_n;
  const int proc_id = ctx->proc_id;
  const int mutexl = ctx->args->mutexl;

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
                            getpid(), getppid(), 0);
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

    int s = receive(ctx, i, &response);

    if (s || response.s_header.s_type != STARTED) {
      fprintf(stderr, "error: %i unable to receive STARTED from id=%d\n",
              ctx->proc_id, i);
    }
  }

  // log received all started
  payload_len =
      sprintf(request.s_payload, log_received_all_started_fmt, 0, proc_id);

  logs_event_write(ctx, request.s_payload, payload_len);

  // effective work
  int total_iterations = ctx->proc_id * 5;
  for (int i = 0; i < total_iterations; ++i) {
    payload_len = sprintf(request.s_payload, log_loop_operation_fmt,
                          ctx->proc_id, i + 1, total_iterations);

    if (mutexl) {
      request_cs(ctx);
    }

    print(request.s_payload);

    if (mutexl) {
      release_cs(ctx);
    }
  }

  // send done
  payload_len = sprintf(request.s_payload, log_done_fmt, 0, proc_id, 0);
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

    int s = receive(ctx, i, &response);

    if (s || response.s_header.s_type != DONE) {
      fprintf(stderr, "error: %i unable to receive DONE from id=%d\n",
              ctx->proc_id, i);
    }
  }

  // log received all done
  payload_len =
      sprintf(request.s_payload, log_received_all_done_fmt, 0, proc_id);

  logs_event_write(ctx, request.s_payload, payload_len);

  return 0;
}

static int proc_run_all(ctx *ctx) {
  for (int i = 1; i <= ctx->args->proc_n; ++i) {
    int pid = fork();

    if (pid == 0) {
      ctx->proc_id = i;
      return proc_fn(ctx);
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

  return s == -1 ? -1 : 0;
}

int send_multicast(void *self, const Message *msg) {
  ctx *ctx = self;
  int n = ctx->args->proc_n;

  int status = 0;

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

  ctx *ctx = self;
  int rfd = pipes_reader_fd(ctx, from);

  int status = -1;

  memset(msg->s_payload, 0, msg->s_header.s_payload_len);

  while (1) {
    int s = read(rfd, msg, sizeof(MessageHeader));

    if (s == -1) {
      nanosleep(&s_ts, NULL);
      continue;
    }
    if (s == 0) {
      break;
    }

    s = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
    if (s > 0) {
      status = 0;
    }
    break;
  }
  return status;
}

int receive_any(void *self, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  ctx *ctx = self;
  const int n = ctx->args->proc_n;
  const int n_eofs = ctx->proc_id == 0 ? n : n - 1;

  int status = -1;

  memset(msg->s_payload, 0, msg->s_header.s_payload_len);

  for (int id_from = 1, eofs = 0; eofs < n_eofs; ++id_from) {
    // sleep and loop again
    if (id_from > n) {
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

    s = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
    if (s > 0) {
      status = 0;
    }
    break;
  }
  return status;
}

int request_cs(const void *self) {
  // TODO
  return 0;
}
int release_cs(const void *self) {
  // TODO
  return 0;
}

int main(int argc, const char **argv) {
  int status;

  args args = {
      .proc_n = 0,
      .mutexl = 0,
      .events_path = events_log,
      .pipes_path = pipes_log,
  };
  ctx ctx = {
      .args = &args,
      .events_f = NULL,
      .pipes_f = NULL,
      .pipes = NULL,
      .proc_id = 0, // parent
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

  if ((status = proc_run_all(&ctx))) {
    goto cleanup;
  }

  if (parent_is(&ctx)) {
    status = parent_fn(&ctx);
  }

cleanup:
  pipes_close(&ctx);

  logs_close(&ctx);

  return status;
}
