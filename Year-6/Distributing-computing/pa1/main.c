#include "argparse.h"
#include "common.h"
#include <assert.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

typedef enum codes {
  CODES_OK = 0,
  CODES_BAD_ALLOC = 1,
  CODES_BAD_PIPE = 2,
  CODES_BAD_FILE = 3,
  CODES_BAD_USAGE = 255,
} codes;

typedef struct args {
  int nproc;
  const char *pipes_path;
  const char *events_path;
} args;

typedef struct ctx_pipe {
  int r;
  int w;
} ctx_pipe;

typedef struct ctx {
  const args *args;

  FILE *events_f;
  FILE *pipes_f;

  ctx_pipe *pipes;
} ctx;

int args_parse(args *args, int argc, const char **argv) {
  static const char *const s_usages[] = {
      "app [options]",
      NULL,
  };

  struct argparse_option options[] = {
      OPT_GROUP("Options:"),
      OPT_HELP(),
      OPT_INTEGER('p', "nproc", &args->nproc, "number of processes"),
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

void logs_close(ctx *ctx) {
  if (ctx->events_f) {
    fclose(ctx->events_f);
  }
  if (ctx->pipes_f) {
    fclose(ctx->pipes_f);
  }
}

int logs_open(ctx *ctx) {
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

ctx_pipe *pipes_select(const ctx *ctx, int i, int j) {
  return ctx->pipes + ((i * ctx->args->nproc) + j);
}

void pipes_close1(ctx_pipe *p) {
  if (p->r) {
    close(p->r);
  }
  if (p->w) {
    close(p->w);
  }
}

void pipes_close(ctx *ctx) {
  int nproc = ctx->args->nproc;

  if (ctx->pipes) {
    for (int i = 0; i < nproc; ++i) {
      for (int j = 0; j < nproc; ++j) {
        if (i != j) {
          ctx_pipe *p = pipes_select(ctx, i, j);
          pipes_close1(p);
        }
      }
    }
    free(ctx->pipes);
  }
}

int pipes_open(ctx *ctx) {
  int nproc = ctx->args->nproc;

  ctx->pipes = calloc(1, sizeof(*ctx->pipes) * nproc * nproc);

  if (!ctx->pipes) {
    return CODES_BAD_ALLOC;
  }

  for (int i = 0; i < nproc; ++i) {
    for (int j = 0; j < nproc; ++j) {
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

int main(int argc, const char **argv) {
  int code;

  args args = (struct args){
      .nproc = 0,
      .events_path = events_log,
      .pipes_path = pipes_log,
  };
  ctx ctx = (struct ctx){
      .args = &args,
      .events_f = NULL,
      .pipes_f = NULL,
      .pipes = NULL,
  };

  if ((code = args_parse(&args, argc, argv))) {
    return code;
  }

  if ((code = logs_open(&ctx))) {
    return code;
  }

  if ((code = pipes_open(&ctx))) {
    return code;
  }

  pipes_close(&ctx);

  logs_close(&ctx);

  return code;
}
