#pragma once

typedef struct hir_struct {
  list_hir_subroutine *subroutines;
} hir;

hir *hir_new();
void hir_free(hir *self);
