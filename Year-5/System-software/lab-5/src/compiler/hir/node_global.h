#pragma once

#include "compiler/hir/node.h"
#include "compiler/hir/node_stmt.h"

// SUBROUTINE
typedef struct hir_subroutine {
  hir_base        base;
  hir_id         *id;
  list_hir_param *params;
  hir_type_base  *ret_type;
  list_hir_var   *vars;
  hir_stmt_block *body;
  hir_symbol     *binding_ref;
} hir_subroutine;

hir_subroutine *hir_subroutine_new(span *span, hir_id *id,
                                   list_hir_param *params,
                                   hir_type_base *ret_type, list_hir_var *vars,
                                   void *body, hir_symbol *binding_ref);
void            hir_subroutine_free(hir_subroutine *self);

static inline void container_delete_hir_subroutine(void *data) {
  hir_subroutine_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_subroutine, hir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_hir_subroutine);
