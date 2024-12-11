#include "context.h"

#include "util/macro.h"
#include <stdlib.h>

context *context_new() {
  context *self = MALLOC(context);
  self->s_debugger = STATE_DEBUGGER_IDLE;
  self->s_target = STATE_TARGET_NO_TARGET;
  self->buf_out = string_buffer_new(0, 0);
  self->buf_err = string_buffer_new(0, 0);

  self->name = NULL;
  self->abfd = NULL;
  self->abfd_asymbols = NULL;
  return self;
}

void context_free(context *self) {
  if (self) {
    string_buffer_free(self->buf_out);
    string_buffer_free(self->buf_err);
    if (self->name) {
      free(self->name);
    }
    if (self->abfd_asymbols) {
      free(self->abfd_asymbols);
    }
    if (self->abfd) {
      bfd_close(self->abfd);
    }
    free(self);
  }
}
