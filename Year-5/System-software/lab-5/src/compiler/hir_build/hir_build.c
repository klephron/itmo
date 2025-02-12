#include "hir_build.h"

#include "compiler/hir_build/lower_ast.h"

hir_build_result hir_build(const list_ast *asts) {
  hir_build_initial_result initial_result = hir_build_initial(asts);

  return (hir_build_result){
      .hir = initial_result.hir,
      .symbol_table = NULL,
      .type_table = NULL,
      .exceptions = initial_result.exceptions,
  };
}
