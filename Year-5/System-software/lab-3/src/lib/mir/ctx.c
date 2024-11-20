#include "mir/ctx.h"
#include "util/macro.h"

mir_ctx *mir_ctx_new(mir *mir) {
  mir_ctx *self = MALLOC(mir_ctx);
  self->mir = mir;
  self->exceptions = list_exception_new();
  return self;
}

void mir_ctx_free(mir_ctx *self) {
  if (self) {
    self->mir = NULL;
    list_exception_free(self->exceptions);
    free(self);
  }
}

void mir_ctx_build(mir_ctx *self, eir *eir) {
  UNUSED(self);
  UNUSED(eir);
}
