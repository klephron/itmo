#pragma once

#include "compiler/hir/node_class.h"
#include "compiler/hir/node_subroutine.h"
#include "compiler/symbol_table/symbol_table.h"
#include "compiler/type_table/type_table.h"

typedef struct hir_struct {
  list_hir_class      *classes;
  list_hir_subroutine *subroutines;
  const symbol_table  *symbol_table_ref;
  const type_table    *type_table_ref;
} hir;

hir *hir_new();
void hir_free(hir *self);
