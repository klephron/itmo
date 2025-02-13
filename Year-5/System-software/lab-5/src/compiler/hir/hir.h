#pragma once

#include "compiler/hir/node_subroutine.h"
#include "compiler/resolve/symbol_table.h"
#include "compiler/resolve/type_table.h"

typedef struct hir_struct {
  list_hir_subroutine *subroutines;
  const symbol_table  *symbol_table_ref;
  const type_table    *type_table_ref;
} hir;

hir *hir_new();
void hir_free(hir *self);
