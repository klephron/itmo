#include "bind_types.h"

hir_bind_types_result hir_bind_types(hir *hir) {
  return (hir_bind_types_result){
      .type_table = NULL,
      .exceptions = NULL,
  };
}
