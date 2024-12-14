#include "cmd.h"

#include "util/macro.h"

void cmd_exit(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(rest);

  ctx->s_debugger = STATE_DEBUGGER_EXITING;
}

const char *cmd_exit_desc = "Exit debugger";
