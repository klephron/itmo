#pragma once

#include "compiler/hir/node_global.h"
#include "compiler/hir/symbols.h"

typedef struct hir_struct {
  list_hir_symbol     *globals;
  list_hir_symbol     *locals;
  list_hir_subroutine *subroutines;
} hir;

hir *hir_new();
void hir_free(hir *self);
