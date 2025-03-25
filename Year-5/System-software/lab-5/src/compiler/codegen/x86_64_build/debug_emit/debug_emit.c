#include "debug_emit.h"

#include "util/macro.h"

cg_debug_emit_result cg_debug_emit(cg_x86_64 *code, const cg_debug *debug) {
  cg_debug_emit_result result = {
      .exceptions = list_exception_new(),
  };

  UNUSED(code);
  UNUSED(debug);

  return result;
}
