#include "str.h"
#include "util/log.h"

const char *state_target_enum_str(state_target_enum state) {
  switch (state) {
  case STATE_TARGET_NO_TARGET:
    return "NO_TARGET";
  case STATE_TARGET_NOT_RUNNING:
    return "NOT_RUNNING";
  case STATE_TARGET_RUNNING:
    return "RUNNING";
  case STATE_TARGET_UNKNOWN:
  default:
    warn("unexpected state %d", state);
    return "UNKNOWN";
  }
}

const char *state_debugger_enum_str(state_debugger_enum state) {
  switch (state) {
  case STATE_DEBUGGER_IDLE:
    return "IDLE";
  case STATE_DEBUGGER_PROCESSING:
    return "PROCESSING";
  case STATE_DEBUGGER_ERROR:
    return "ERROR";
  case STATE_DEBUGGER_EXITING:
    return "EXITING";
  case STATE_DEBUGGER_UNKNOWN:
  default:
    warn("unexpected state %d", state);
    return "UNKNOWN";
  }
}
