#include "argparse.h"
#include "common.h"
#include "ipc.h"
#include <assert.h>
#include <fcntl.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <unistd.h>

typedef enum codes {
  CODES_OK = 0,
  CODES_BAD_ALLOC = 1,
  CODES_BAD_PIPE = 2,
  CODES_BAD_FILE = 3,
  CODES_BAD_FORK = 4,
  CODES_BAD_USAGE = 255,
} codes;

typedef struct args {
  int proc_n;
  const char *pipes_path;
  const char *events_path;
} args;

typedef struct ctx_pipe {
  int r;
  int w;
} ctx_pipe;

typedef enum ctx_pipe_type {
  PIPE_UNUSED = 0,
  PIPE_READER,
  PIPE_WRITER,
} ctx_pipe_type;

typedef struct ctx {
  const args *args;

  FILE *events_f;
  FILE *pipes_f;

  ctx_pipe *pipes;

  int proc_id;
} ctx;

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

  int code = argparse_parse(&argparse, argc, argv);

  if (code) {
    argparse_usage(&argparse);
    return CODES_BAD_USAGE;
  }
  return code;
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
  int code = 0;

  if (!(ctx->events_f = fopen(ctx->args->events_path, "w"))) {
    code = CODES_BAD_FILE;
  };
  if (!(ctx->pipes_f = fopen(ctx->args->pipes_path, "w"))) {
    code = CODES_BAD_FILE;
  };

  if (code) {
    logs_close(ctx);
  }

  return code;
}

static ctx_pipe *pipes_select(const ctx *ctx, int i, int j) {
  return ctx->pipes + ((i * ctx->args->proc_n) + j);
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
        ctx_pipe *p = pipes_select(ctx, i, j);
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
    return CODES_BAD_ALLOC;
  }

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      if (i != j) {
        ctx_pipe *p = pipes_select(ctx, i, j);
        if (pipe((int *)p) < 0) {
          goto error;
        }
        fprintf(ctx->pipes_f, "%d/%d\n", p->r, p->w);
      }
    }
  }

  fflush(ctx->pipes_f);
  return 0;

error:
  fflush(ctx->pipes_f);
  pipes_close(ctx);
  return CODES_BAD_PIPE;
}

static int pipes_type(ctx *ctx, int i, int j) {
  if (ctx->proc_id) {
    if (ctx->proc_id - 1 == i) {
      return PIPE_WRITER;
    }
    if (ctx->proc_id - 1 == j) {
      return PIPE_READER;
    }
  } else {
    if (i == j) {
      return PIPE_READER;
    }
  }
  return PIPE_UNUSED;
}

// closes and configures all pipes
static void pipes_configure(ctx *ctx) {
  int n = ctx->args->proc_n;

  for (int i = 0; i < n; ++i) {
    for (int j = 0; j < n; ++j) {
      ctx_pipe *p = pipes_select(ctx, i, j);

      switch (pipes_type(ctx, i, j)) {
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

static int parent_is(ctx *ctx) {
  return ctx->proc_id == 0;
}

static int parent_fn(ctx *ctx) {
  int n = ctx->args->proc_n;

  Message message = {
      .s_header =
          {
              .s_magic = MESSAGE_MAGIC,
              .s_payload_len = MAX_PAYLOAD_LEN,
              .s_type = 0,
              .s_local_time = 0,
          },
      .s_payload = {0},
  };

  pipes_configure(ctx);

  // receive started
  for (int i = 0; i < n; ++i) {
    receive_any(ctx, &message);
  }

  // receive done
  for (int i = 0; i < n; ++i) {
    receive_any(ctx, &message);
  }

  int wpid;
  int status = 0;

  while ((wpid = wait(&status)) > 0)
    ;

  return 0;
}

static int proc_fn(ctx *ctx) {
  pipes_configure(ctx);

  // TODO:

  return 0;
}

static int proc_run_all(ctx *ctx) {
  for (int i = 1; i <= ctx->args->proc_n; ++i) {
    int pid = fork();

    if (pid == 0) {
      ctx->proc_id = i;
      return proc_fn(ctx);
    } else if (pid < 0) {
      return CODES_BAD_FORK;
    }
  }
  return 0;
}

int send(void *self, local_id dst, const Message *msg) {
  // TODO:
  return 0;
}

int send_multicast(void *self, const Message *msg) {
  // TODO:
  return 0;
}

int receive(void *self, local_id from, Message *msg) {
  // TODO:
  return 0;
}

int receive_any(void *self, Message *msg) {
  // TODO:
  return 0;
}

int main(int argc, const char **argv) {
  int code;

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

  if ((code = args_parse(&args, argc, argv))) {
    return code;
  }

  if ((code = logs_open(&ctx))) {
    goto cleanup;
  }

  if ((code = pipes_open(&ctx))) {
    goto cleanup;
  }

  if ((code = proc_run_all(&ctx))) {
    goto cleanup;
  }

  if (parent_is(&ctx)) {
    code = parent_fn(&ctx);
  }

cleanup:
  pipes_close(&ctx);

  logs_close(&ctx);

  return code;
}
