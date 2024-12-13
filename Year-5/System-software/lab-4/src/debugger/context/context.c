#include "context.h"

#include "debugger/context/str.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdlib.h>
#include <sys/wait.h>

context *context_new() {
  context *self = MALLOC(context);
  self->s_debugger = STATE_DEBUGGER_IDLE;
  self->buf_out = string_buffer_new(0, 0);
  self->buf_err = string_buffer_new(0, 0);

  self->fname = NULL;
  self->abfd = NULL;
  self->abfd_asymbols = NULL;
  self->abfd_asymbols_count = 0;
  self->pid = 0;

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

state_target_enum context_get_state_target(context *self) {
  state_target_enum state = STATE_TARGET_UNKNOWN;

  if (!self->fname) {
    state = STATE_TARGET_NO_TARGET;
  } else if (!self->pid) {
    state = STATE_TARGET_NOT_RUNNING;
  } else {
    int   status;
    pid_t child_pid = waitpid(self->pid, &status, WNOHANG);
    if (child_pid == self->pid) {
      state = STATE_TARGET_NOT_RUNNING;
    } else if (child_pid == 0) {
      state = STATE_TARGET_RUNNING;
    }
  }
  if (state == STATE_TARGET_NOT_RUNNING) {
    self->pid = 0;
  }
  debug("target state: %s", state_target_enum_str(state));
  return state;
}
