#include "ctx.h"
#include "exception/container.h"
#include "util/macro.h"

eir_ctx *eir_ctx_new(eir *eir) {
  eir_ctx *self = MALLOC(eir_ctx);
  self->eir = eir;
  self->exceptions = list_exception_new();
  return self;
}

void eir_ctx_reset_exceptions(eir_ctx *self) {
  list_exception_free(self->exceptions);
  self->exceptions = list_exception_new();
}

void eir_ctx_free(eir_ctx *self) {
  if (self) {
    list_exception_free(self->exceptions);
    self->exceptions = NULL;
    self->eir = NULL;
    free(self);
  }
}

void eir_ctx_build(eir_ctx *self, const hir *hir) {
  self->eir->globals = hir->globals;
  self->eir->locals = hir->locals;
}
