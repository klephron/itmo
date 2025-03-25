#include "cmd.h"

#include "util/macro.h"

static const char *DESC = "Exit debugger";

static const char *HELP = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(rest);

  ctx->s_debugger = DEBUGGER_EXITING;
}

cmd *cmd_exit(const char *cmd_ref) {
  return cmd_entry_new(fn, DESC, HELP, cmd_ref);
}
