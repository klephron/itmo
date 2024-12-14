#include "cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"
#include <stdlib.h>

cmd_entry *cmd_entry_new(cmd_fn fn, const char *desc_ref, const char *cmd_ref) {
  cmd_entry *self = MALLOC(cmd_entry);
  self->fn = fn;
  self->desc_ref = desc_ref;
  self->cmd_ref = cmd_ref;
  return self;
}

void cmd_entry_free(cmd_entry *self) {
  if (self) {
    free(self);
  }
}

int cmd_error_if_no_target(context *ctx) {
  if (context_get_target_state(ctx) == STATE_TARGET_NO_TARGET) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not loaded");
    return 1;
  }
  return 0;
}
