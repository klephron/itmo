#include "hir_build.h"

#include "compiler/hir_build/bind_symbols.h"
#include "compiler/hir_build/bind_types.h"
#include "compiler/hir_build/expand_templates.h"
#include "compiler/hir_build/lower_ast.h"

hir_build_result hir_build(const list_ast *asts, int ignore_errors) {
  hir_build_result result = {
      .hir = NULL,
      .symbol_table = NULL,
      .type_table = NULL,
      .exceptions = list_exception_new(),
  };

  if (result.exceptions &&
      (list_exception_empty(result.exceptions) || ignore_errors)) {

    hir_build_initial_result r = hir_build_initial(asts);

    list_exception_extend(result.exceptions, r.exceptions);
    result.hir = r.hir;
  }

  if (result.exceptions &&
      (list_exception_empty(result.exceptions) || ignore_errors)) {

    hir_bind_types_result r = hir_bind_types(result.hir);

    list_exception_extend(result.exceptions, r.exceptions);
    result.type_table = r.type_table;
  }

  if (result.exceptions &&
      (list_exception_empty(result.exceptions) || ignore_errors)) {

    hir_expand_templates_result r =
        hir_expand_templates(result.hir, result.type_table);

    list_exception_extend(result.exceptions, r.exceptions);
    hir_free(result.hir);
    result.hir = r.hir;
  }

  if (result.exceptions &&
      (list_exception_empty(result.exceptions) || ignore_errors)) {

    hir_bind_symbols_result r = hir_bind_symbols(result.hir);

    list_exception_extend(result.exceptions, r.exceptions);
    result.symbol_table = r.symbol_table;
  }

  return result;
}
