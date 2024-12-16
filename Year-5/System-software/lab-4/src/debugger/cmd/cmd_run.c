#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

static const char *desc = "Run target";

static const char *help = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char  *args = NULL;
  char **argv = NULL;

  args = trim(strdup(rest));

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  if (context_tg_is_running(ctx)) {
    context_error(ctx, "target is already running");
    goto cleanup;
  }

  // parse arguments to command
  size_t size = 4;
  size_t argc = 0;
  argv = malloc(size * sizeof(char *));
  char *token = strtok(args, " ");
  while (token) {
    if (argc >= size - 1) {
      size *= 2;
      argv = realloc(argv, size * sizeof(char *));
    }
    argv[argc++] = strdup(token);
    token = strtok(NULL, " ");
  }
  argv[argc] = NULL;

  if (context_tg_run(ctx, argv) == TARGET_ERROR) {
    context_error(ctx, "target is not created");
    goto cleanup;
  }

  char buf[64];
  context_out_append(ctx, "started with pid ");
  snprintf(buf, STRMAXLEN(buf), "%d", ctx->pid);
  context_out_appendln(ctx, buf);

  for (hashset_breakpoint_it it = hashset_breakpoint_begin(ctx->breaks);
       !it.end(&it); it.next(&it)) {
    breakpoint *bp = it.get(&it);
    bp->is_placed = 0;
    context_bp_place(ctx, bp);
  }

cleanup:
  if (argv) {
    for (size_t i = 0; argv[i] != NULL; ++i) {
      free(argv[i]);
    }
    free(argv);
  }
  if (args) {
    free(args);
  }
}

cmd *cmd_run(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
