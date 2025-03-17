#include "emit.h"
#include "util/macro.h"
#include <string.h>

char *cg_x86_64_emit_gas(cg_x86_64_emit_ctx *emit_ctx, const cg_x86_64 *code) {
  UNUSED(emit_ctx);
  UNUSED(code);
  return strdup("");
}
