#pragma once

#include "compiler/type/type.h"

typedef enum hir_binding_enum {
  HIR_BINDING_UNKNOWN,
  HIR_BINDING_GLOBAL,     // binded as global symbol
  HIR_BINDING_SUBROUTINE, // binded inside subroutine
} hir_binding_enum;

typedef struct hir_symbol_struct {
  char                           *name;
  hir_binding_enum                bind_kind;
  const struct hir_symbol_struct *bind_parent;
  type_base                      *type;
  span                           *decl_span;
} hir_symbol;

hir_symbol *hir_symbol_new(char *name, hir_binding_enum bind_kind,
                           const hir_symbol *bind_parent, type_base *type,
                           span *decl_span);
void        hir_symbol_free(hir_symbol *self);

static inline void container_delete_hir_symbol(void *data) {
  hir_symbol_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_symbol, hir_symbol, container_cmp_false,
                           container_new_move, container_delete_hir_symbol);
