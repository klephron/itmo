#include "context.h"

#include "util/macro.h"
#include <stdlib.h>

context *context_new() {
  context *self = MALLOC(context);
  self->s_debugger = STATE_DEBUGGER_IDLE;
  self->s_target = STATE_TARGET_NOT_RUNNING;
  self->buffer_out = string_buffer_new(0, 0);
  self->buffer_err = string_buffer_new(0, 0);
  return self;
}

void context_free(context *self) {
  if (self) {
    string_buffer_free(self->buffer_out);
    string_buffer_free(self->buffer_err);
    free(self);
  }
}
