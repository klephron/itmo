#include "cmd.h"

#include "util/macro.h"

void cmd_exit(context *ctx, const char *rest) {
  UNUSED(rest);

  ctx->s_debugger = STATE_DEBUGGER_EXITING;
}
