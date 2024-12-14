#include "cmd.h"

#include "debugger/context/context.h"

int cmd_error_if_no_target(context *ctx) {
  if (context_get_target_state(ctx) == STATE_TARGET_NO_TARGET) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not loaded");
    return 1;
  }
  return 0;
}
