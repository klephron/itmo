#define _POSIX_C_SOURCE 199309L

#include "argparse.h"
#include "common.h"
#include "ipc.h"
#include "pa1.h"

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
  const char *pipes_path;
  const char *events_path;
} args;

typedef struct ctx_pipe_t {
  int r;
  int w;
} ctx_pipe;

typedef enum ctx_pipe_type_t {
  PIPE_UNUSED = 0,
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
      OPT_INTEGER('p', "nproc", &args->proc_n, "number of processes"),
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

static ctx_pipe *pipes_select(const ctx *ctx, ctx_pipe_idx idx) {
  return ctx->pipes + ((idx.i * ctx->args->proc_n) + idx.j);
}

static ctx_pipe_idx pipes_idx(const ctx *ctx, ctx_pipe *p) {
  int n = ctx->args->proc_n;

  int idx = p - ctx->pipes;
  int i = idx / n;
  int j = idx % n;
  return (ctx_pipe_idx){.i = i, .j = j};
}

static void pipes_close1(ctx_pipe *p) {
  if (p->r) {
    close(p->r);
  }
  if (p->w) {
    close(p->w);
  }
}

static void pipes_close(ctx *ctx) {
  int n = ctx->args->proc_n;

  if (ctx->pipes) {
    for (int i = 0; i < n; ++i) {
      for (int j = 0; j < n; ++j) {
        ctx_pipe *p = pipes_select(ctx, (ctx_pipe_idx){.i = i, .j = j});
        pipes_close1(p);
      }
    }
    free(ctx->pipes);
    ctx->pipes = NULL;
  }
}

static int pipes_open(ctx *ctx) {
  int n = ctx->args->proc_n;

  ctx->pipes = calloc(1, sizeof(*ctx->pipes) * n * n);

  if (!ctx->pipes) {
    return STATUS_BAD_ALLOC;
  }

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      ctx_pipe *p = pipes_select(ctx, (ctx_pipe_idx){.i = i, .j = j});
      if (pipe((int *)p) < 0) {
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

static int pipes_type(ctx *ctx, ctx_pipe_idx idx) {
  if (ctx->proc_id) {
    if (ctx->proc_id - 1 == idx.i) {
      return PIPE_WRITER;
    }
    if (ctx->proc_id - 1 == idx.j) {
      return PIPE_READER;
    }
  } else {
    if (idx.i == idx.j) {
      return PIPE_READER;
    }
  }
  return PIPE_UNUSED;
}

static int pipes_type_pipe(ctx *ctx, ctx_pipe *p) {
  return pipes_type(ctx, pipes_idx(ctx, p));
}

// closes and configures all pipes
static void pipes_configure(ctx *ctx) {
  int n = ctx->args->proc_n;

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      ctx_pipe_idx idx = (ctx_pipe_idx){.i = i, .j = j};
      ctx_pipe *p = pipes_select(ctx, idx);

      switch (pipes_type(ctx, idx)) {
      case PIPE_READER: {
        int rfs = fcntl(p->r, F_GETFL);
        rfs |= O_NONBLOCK;
        fcntl(p->r, F_SETFL, rfs);
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

// match proc_id with idx cuz they encode differently
// 1. to write to proc_id=1 -> idx{i=(1-1)}
// 2. to read from proc_id=1 -> idx{j=(1-1)}
// 3. to write to parent (proc_id=0) -> idx{i=j}
static ctx_pipe *pipes_select_proc(ctx *ctx, int id_from, int id_to) {
  if (id_to == 0) {
    id_to = id_from;
  }
  int i = id_from - 1;
  int j = id_to - 1;
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
    int r = receive_any(ctx, &message);
    if (r) {
      fprintf(stderr, "error: parent unable to receive_any\n");
    }
  }

  // receive done
  for (int i = 0; i < n; ++i) {
    int r = receive_any(ctx, &message);
    if (r) {
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
  int payload_len =
      sprintf(request.s_payload, log_started_fmt, proc_id, getpid(), getppid());
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

    int r = receive(ctx, i, &response);

    if (r || response.s_header.s_type != STARTED) {
      fprintf(stderr, "error: %i unable to receive STARTED from id=%d\n",
              ctx->proc_id, i);
    }
  }

  // log received all started
  payload_len =
      sprintf(request.s_payload, log_received_all_started_fmt, proc_id);

  logs_event_write(ctx, request.s_payload, payload_len);

  // send done
  payload_len = sprintf(request.s_payload, log_done_fmt, proc_id);
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

    int r = receive(ctx, i, &response);

    if (r || response.s_header.s_type != DONE) {
      fprintf(stderr, "error: %i unable to receive DONE from id=%d\n",
              ctx->proc_id, i);
    }
  }

  // log received all done
  payload_len = sprintf(request.s_payload, log_received_all_done_fmt, proc_id);

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
  int r = write(wfd, msg, msg->s_header.s_payload_len + sizeof(MessageHeader));

  return r == -1 ? -1 : 0;
}

int send_multicast(void *self, const Message *msg) {
  ctx *ctx = self;
  int n = ctx->args->proc_n;

  int ret = 0;

  // to children and parent
  for (int id_to = 0; id_to <= n; ++id_to) {
    if (ctx->proc_id == id_to) {
      continue;
    }
    if (send(ctx, id_to, msg) == -1) {
      ret = -1;
    }
  }

  return ret;
}

int receive(void *self, local_id from, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  ctx *ctx = self;
  int rfd = pipes_reader_fd(ctx, from);

  int ret = -1;

  memset(msg->s_payload, 0, msg->s_header.s_payload_len);

  while (1) {
    int r = read(rfd, msg, sizeof(MessageHeader));

    if (r > 0) {
      r = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
      if (r > 0) {
        ret = 0;
      }
      break;
    }

    if (r == 0) {
      break;
    }

    nanosleep(&s_ts, NULL);
  }
  return ret;
}

int receive_any(void *self, Message *msg) {
  static struct timespec s_ts = {
      .tv_sec = 0,
      .tv_nsec = 1000000,
  };

  ctx *ctx = self;
  const int n = ctx->args->proc_n;
  const int n_eofs = ctx->proc_id == 0 ? n : n - 1;

  int ret = -1;

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
    int r = read(rfd, msg, sizeof(MessageHeader));

    if (r == 0) {
      ++eofs;
      continue;
    }
    if (r == -1) {
      continue;
    }

    r = read(rfd, msg->s_payload, msg->s_header.s_payload_len);
    if (r > 0) {
      ret = 0;
    }
    break;
  }
  return ret;
}

int main(int argc, const char **argv) {
  int status;

  args args = {
      .proc_n = 0,
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
