#pragma once

#include "util/string_buffer.h"
#include <bfd.h>
#include <sys/types.h>

typedef enum debugger_state_enum {
  DEBUGGER_UNKNOWN,
  DEBUGGER_IDLE, // waiting for user input
  DEBUGGER_PROCESSING,
  DEBUGGER_ERROR,
  DEBUGGER_EXITING,
} debugger_state_enum;

typedef enum target_state_enum {
  TARGET_UNKNOWN,
  TARGET_NO_TARGET,
  TARGET_NOT_RUNNING,
  TARGET_RUNNING,
} target_state_enum;

typedef struct context_struct {
  debugger_state_enum s_debugger;
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

target_state_enum context_get_target_state(context *self);
