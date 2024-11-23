#include "codegen/x86_64.h"
#include "util/macro.h"
#include <string.h>

void x86_64_free(x86_64 *self) {
  if (self) {
    free(self);
  }
}

x86_64 *x86_64_build(mir *mir) {
  UNUSED(mir);
  x86_64 *self = MALLOC(x86_64);
  return self;
}

char *x86_64_emit(x86_64 *self) {
  UNUSED(self);
  return strdup("assembly code");
}
