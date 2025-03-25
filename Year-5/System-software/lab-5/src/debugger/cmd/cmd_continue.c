#include "cmd.h"

#include "util/macro.h"

#include <stdlib.h>
#include <sys/ptrace.h>

static const char *DESC = "Continue target execution";

static const char *HELP = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(rest);
  char                   *args = NULL;
  struct user_regs_struct regs;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  if (context_tg_continue(ctx) == TARGET_ERROR) {
    context_error(ctx, "error while continue");
  }

  if (context_tg_is_running(ctx)) {
    if (context_tg_getregs(ctx, &regs) == TARGET_ERROR) {
      context_error(ctx, "failed to get regs");
      goto cleanup;
    }
    debug_line curr = ctx->line;
    curr.address    = regs.rip;
    if (context_dbg_line_by_address(ctx, &curr) != -1) {
      context_dbg_line_set(ctx, curr);
    }

    cmd_ctx_out_line(ctx, curr);
  }

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_continue(const char *cmd_ref) {
  return cmd_entry_new(fn, DESC, HELP, cmd_ref);
}
