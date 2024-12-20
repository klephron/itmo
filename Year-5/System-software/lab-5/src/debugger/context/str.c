#include "str.h"
#include "util/log.h"

const char *state_target_enum_str(target_state_enum state) {
  switch (state) {
  case TARGET_UNKNOWN:
    return "UNKNOWN";
  case TARGET_NO_TARGET:
    return "NO_TARGET";
  case TARGET_NOT_RUNNING:
    return "NOT_RUNNING";
  case TARGET_ERROR:
    return "ERROR";
  case TARGET_BRKPT:
    return "BRKPT";
  case TARGET_TRAPPED:
    return "TRAPPED";
  }
  return "UNKNOWN";
}

const char *state_debugger_enum_str(debugger_state_enum state) {
  switch (state) {
  case DEBUGGER_IDLE:
    return "IDLE";
  case DEBUGGER_PROCESSING:
    return "PROCESSING";
  case DEBUGGER_ERROR:
    return "ERROR";
  case DEBUGGER_EXITING:
    return "EXITING";
  case DEBUGGER_UNKNOWN:
  default:
    warn("unexpected state %d", state);
    return "UNKNOWN";
  }
}
