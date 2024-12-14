#include "cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"
#include <stdlib.h>

cmd *cmd_entry_new(cmd_fn fn, const char *desc_ref, const char *help_ref,
                   const char *cmd_ref) {
  cmd *self = MALLOC(cmd);
  self->fn = fn;
  self->desc_ref = desc_ref;
  self->help_ref = help_ref;
  self->cmd_ref = cmd_ref;
  return self;
}

void cmd_entry_free(cmd *self) {
  if (self) {
    free(self);
  }
}

int cmd_error_if_no_target(context *ctx) {
  if (context_get_target_state(ctx) == TARGET_NO_TARGET) {
    ctx->s_debugger = DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not loaded");
    return 1;
  }
  return 0;
}
