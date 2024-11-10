#pragma once

#include "hir/symbols.h"
#include "type/type.h"

typedef enum hir_node_enum {
  HIR_NODE_UNKNOWN,
  HIR_NODE_LIT,
  HIR_NODE_SUBROUTINE,
  HIR_NODE_ROOT,
} hir_node_enum;

typedef struct hir_base_struct {
  hir_node_enum kind;
  span         *span;
} hir_base;

// LITERAL
typedef union hir_lit_u_union {
  hir_base *v_node;
  long      v_long;
  ulong     v_ulong;
  char     *v_str;
  char     *v_char;
  int       v_bool;
} hir_lit_u;

typedef struct hir_lit_struct {
  hir_base   base;
  type_base *type;
  hir_lit_u  value;
} hir_lit;

hir_lit *hir_lit_new(span *span, type_base *type, hir_lit_u value);

// SUBROUTINE
typedef struct hir_subroutine {
  hir_base          base;
  const hir_symbol *symbol;
} hir_subroutine;

hir_subroutine *hir_subroutine_new(span *span, const hir_symbol *symbol);
void            hir_subroutine_free(hir_subroutine *self);

static inline void container_delete_hir_subroutine(void *data) {
  hir_subroutine_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_subroutine, hir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_hir_subroutine);

// GENERIC
void hir_node_free(hir_base *generic);

// HIR_ROOT
typedef struct hir_struct {
  list_hir_symbol     *symbols;
  list_hir_subroutine *subroutines;
} hir;

hir *hir_new();
void hir_free(hir *self);
