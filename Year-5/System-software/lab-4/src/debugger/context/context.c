#include "context.h"

#include "util/macro.h"
#include <stdlib.h>

context *context_new() {
  context *self = MALLOC(context);
  self->s_debugger = STATE_DEBUGGER_IDLE;
  self->s_target = STATE_TARGET_NO_TARGET;
  self->buf_out = string_buffer_new(0, 0);
  self->buf_err = string_buffer_new(0, 0);

  self->fname = NULL;
  self->abfd = NULL;
  self->abfd_asymbols = NULL;
  return self;
}

void context_free(context *self) {
  if (self) {
    string_buffer_free(self->buf_out);
    string_buffer_free(self->buf_err);
    if (self->fname) {
      free(self->fname);
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

void context_out_append(context *self, const char *data) {
  string_buffer_append(self->buf_out, data);
}

void context_out_appendln(context *self, const char *data) {
  context_out_append(self, data);
  context_out_append(self, "\n");
}

void context_err_append(context *self, const char *data) {
  string_buffer_append(self->buf_err, data);
}

void context_err_appendln(context *self, const char *data) {
  context_err_append(self, data);
  context_err_append(self, "\n");
}
