#include "mir_build.h"
#include "util/macro.h"

mir_build_result mir_build(const hir *hir, const symbol_table *symbol_table,
                           const type_table *type_table, int ignore_errors) {
  UNUSED(symbol_table);
  UNUSED(type_table);

  mir_build_result result = {
      .mir = mir_new(),
      .exceptions = list_exception_new(),
  };

  return result;
}
