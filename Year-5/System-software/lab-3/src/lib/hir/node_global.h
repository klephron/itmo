#pragma once

#include "hir/node.h"
#include "hir/node_stmt.h"

// SUBROUTINE
typedef struct hir_subroutine {
  hir_base          base;
  hir_id           *id;
  list_hir_param   *params;
  type_base        *ret_type;
  list_hir_var     *vars;
  hir_stmt_base    *body;
  const hir_symbol *symbol;
} hir_subroutine;

hir_subroutine *hir_subroutine_new(span *span, hir_id *id,
                                   list_hir_param *params, type_base *ret_type,
                                   list_hir_var *vars, void *body,
                                   const hir_symbol *symbol);
void            hir_subroutine_free(hir_subroutine *self);

static inline void container_delete_hir_subroutine(void *data) {
  hir_subroutine_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_subroutine, hir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_hir_subroutine);
