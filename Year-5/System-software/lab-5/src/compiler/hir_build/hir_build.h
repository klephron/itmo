#pragma once

#include "compiler/ast/ast.h"
#include "compiler/exception/container.h"
#include "compiler/hir/hir.h"
#include "compiler/resolve/symbol_table.h"
#include "compiler/resolve/type_table.h"

typedef struct hir_build_result_struct {
  hir            *hir;
  symbol_table   *symbol_table;
  type_table     *type_table;
  list_exception *exceptions;
} hir_build_result;

hir_build_result hir_build(const list_ast *asts);
