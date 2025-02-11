#include "hir.h"
#include "util/macro.h"

hir *hir_new() {
  hir *self = MALLOC(hir);
  self->subroutines = list_hir_subroutine_new();
  return self;
}

void hir_free(hir *self) {
  if (self) {
    list_hir_subroutine_free(self->subroutines);
    self->subroutines = NULL;
    free(self);
  }
}
