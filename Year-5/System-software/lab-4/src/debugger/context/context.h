#pragma once

#include "util/string_buffer.h"
#include <bfd.h>
#include <sys/types.h>

typedef enum state_debugger_enum {
  STATE_DEBUGGER_UNKNOWN,
  STATE_DEBUGGER_IDLE, // waiting for user input
  STATE_DEBUGGER_PROCESSING,
  STATE_DEBUGGER_ERROR,
  STATE_DEBUGGER_EXITING,
} state_debugger_enum;

typedef enum state_target_enum {
  STATE_TARGET_UNKNOWN,
  STATE_TARGET_NO_TARGET,
  STATE_TARGET_NOT_RUNNING,
  STATE_TARGET_RUNNING,
} state_target_enum;

typedef struct context_struct {
  state_debugger_enum s_debugger;
  string_buffer      *buf_out;
  string_buffer      *buf_err;
  // internal
  char     *fname;
  bfd      *abfd;
  asymbol **abfd_asymbols;
  int64_t   abfd_asymbols_count;
  pid_t     pid;
} context;

context *context_new();
void     context_free(context *self);

void context_out_append(context *self, const char *data);
void context_out_appendln(context *self, const char *data);
void context_err_append(context *self, const char *data);
void context_err_appendln(context *self, const char *data);

state_target_enum context_get_state_target(context *self);
