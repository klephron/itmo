#include "hir.h"
#include "util/macro.h"

hir *hir_new() {
  hir *self = MALLOC(hir);
  self->globals = list_hir_symbol_new();
  self->locals = list_hir_symbol_new();
  self->subroutines = list_hir_subroutine_new();
  return self;
}

void hir_free(hir *self) {
  if (self) {
    list_hir_symbol_free(self->globals);
    self->globals = NULL;
    list_hir_symbol_free(self->locals);
    self->locals = NULL;
    list_hir_subroutine_free(self->subroutines);
    self->subroutines = NULL;
    free(self);
  }
}
