#include "expand_templates.h"

hir_expand_templates_result hir_expand_templates(const hir  *hir,
                                                 type_table *type_table) {
  hir_expand_templates_result result = {
      .hir = hir_new(),
      .exceptions = list_exception_new(),
  };

  return result;
}
