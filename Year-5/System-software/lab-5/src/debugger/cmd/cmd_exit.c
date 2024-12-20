#include "cmd.h"

#include "util/macro.h"

static const char *desc = "Exit debugger";

static const char *help = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(rest);

  ctx->s_debugger = DEBUGGER_EXITING;
}

cmd *cmd_exit(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
