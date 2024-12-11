#include "cmd.h"
#include "util/macro.h"

void cmd_exit(context *state, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  UNUSED(rest);
  state->s_debugger = STATE_DEBUGGER_EXITING;
}

void cmd_echo(context *state, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  UNUSED(rest);
  string_buffer_append(state->buffer_out, rest);
}
