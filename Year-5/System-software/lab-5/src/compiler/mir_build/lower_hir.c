#include "lower_hir.h"
#include "compiler/exception/list.h"
#include "compiler/mir/mir.h"

mir_lower_hir_result mir_lower_hir(const hir *hir) {
  mir_lower_hir_result result = {
      .mir = mir_new(),
      .exceptions = list_exception_new(),
  };

  return result;
}
