#include "hir.h"
#include "util/macro.h"

hir_subroutine *hir_subroutine_new() {
  hir_subroutine *self = MALLOC(hir_subroutine);
  return self;
}
void hir_subroutine_free(hir_subroutine *self) {
  if (self) {
    free(self);
  }
}

hir *hir_new() {
  hir *self = MALLOC(hir);
  self->symbols = list_hir_symbol_new();
  self->subroutines = list_hir_subroutine_new();
  return self;
}
void hir_free(hir *self) {
  if (self) {
    list_hir_symbol_free(self->symbols);
    self->symbols = NULL;
    list_hir_subroutine_free(self->subroutines);
    self->subroutines = NULL;
    free(self);
  }
}
