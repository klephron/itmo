#include "codegen/x86_64.h"
#include "util/macro.h"
#include <string.h>

void cg_x86_64_free(cg_x86_64 *self) {
  if (self) {
    free(self);
  }
}

cg_x86_64 *cg_x86_64_build(mir *mir) {
  UNUSED(mir);
  cg_x86_64 *self = MALLOC(cg_x86_64);
  return self;
}

char *cg_x86_64_emit(cg_x86_64 *self) {
  UNUSED(self);
  return strdup("assembly code\n");
}
