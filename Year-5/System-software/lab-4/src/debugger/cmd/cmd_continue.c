#include "cmd.h"

#include "util/macro.h"

#include <stdlib.h>
#include <sys/ptrace.h>

static const char *desc = "Continue target execution";

static const char *help = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(rest);
  char *args = NULL;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  if (ptrace(PTRACE_CONT, ctx->pid, NULL, NULL) == -1) {
    cmd_error(ctx, "continue failed");
    goto cleanup;
  }

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_continue(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
