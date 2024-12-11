#pragma once

#include "util/string_buffer.h"

typedef enum state_debugger_enum {
  STATE_DEBUGGER_UNKNOWN,
  STATE_DEBUGGER_IDLE, // waiting for user input
  STATE_DEBUGGER_PROCESSING,
  STATE_DEBUGGER_ERROR,
  STATE_DEBUGGER_EXITING,
} state_debugger_enum;

typedef enum state_target_enum {
  STATE_TARGET_UNKNOWN,
  STATE_TARGET_NOT_RUNNING,
  STATE_TARGET_RUNNING,
  STATE_TARGET_STOPPED,
  STATE_TARGET_EXITED,
  STATE_TARGET_DETACH, // detached from program?
} state_target_enum;

typedef struct context_struct {
  state_debugger_enum s_debugger;
  state_target_enum   s_target;
  string_buffer      *buffer_out;
  string_buffer      *buffer_err;
} context;

context *context_new();
void     context_free(context *self);
