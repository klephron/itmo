#include "context.h"

#include "util/macro.h"
#include "util/string_buffer.h"
#include <stdlib.h>
#include <sys/wait.h>

context *context_new() {
  context *self = MALLOC(context);
  self->s_debugger = DEBUGGER_IDLE;
  self->s_target = TARGET_NO_TARGET;
  self->buf_out = string_buffer_new(0, 0);
  self->buf_err = string_buffer_new(0, 0);

  self->fname = NULL;
  self->abfd = NULL;
  self->abfd_asymbols = NULL;
  self->abfd_asymbols_count = 0;
  self->pid = 0;
  self->breaks = hashset_breakpoint_new();
  self->breaks_cnt = 0;

  self->debug.subroutines_cnt = 0;
  self->debug.subroutines = NULL;
  self->debug.files_cnt = 0;
  self->debug.files = NULL;
  self->debug.lines_cnt = 0;
  self->debug.lines = NULL;
  self->debug.strs_cnt = 0;
  self->debug.strs = NULL;

  self->options.source_lang = false;
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
    hashset_breakpoint_free(self->breaks);
    context_dbg_deinit(self);
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

void context_error(context *ctx, const char *msg) {
  ctx->s_debugger = DEBUGGER_ERROR;
  context_err_appendln(ctx, msg);
}
