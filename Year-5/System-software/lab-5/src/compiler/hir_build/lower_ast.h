#pragma once

#include "compiler/ast/ast.h"
#include "compiler/exception/list.h"
#include "compiler/hir/hir.h"

typedef struct hir_build_initial_ctx_struct {
  hir            *hir_ref;
  list_exception *exceptions;
  const ast      *ast_cur_ref;
} hir_build_initial_ctx;

typedef struct hir_build_initial_result_struct {
  hir            *hir;
  list_exception *exceptions;
} hir_build_initial_result;

hir_build_initial_result hir_build_initial(const list_ast *asts);
