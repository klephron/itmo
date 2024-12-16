#include "cmd.h"

#include "util/macro.h"

#include <stdlib.h>
#include <sys/ptrace.h>

static const char *desc = "Continue target execution";

static const char *help = "Usage: <command>";

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
    cmd_disassemble_at_address(ctx, regs.rip - 1, 1);
  }

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_continue(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
