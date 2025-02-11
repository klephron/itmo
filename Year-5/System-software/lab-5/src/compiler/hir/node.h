#pragma once

#include "compiler/hir/symbols.h"
#include "compiler/type/type.h"

typedef enum hir_node_enum {
  HIR_NODE_UNKNOWN,
  HIR_NODE_ID,
  HIR_NODE_LIT,
  HIR_NODE_PARAM,
  HIR_NODE_VAR,
  HIR_NODE_SUBROUTINE,
  HIR_NODE_STMT,
  HIR_NODE_EXPR,
} hir_node_enum;

typedef struct hir_base_struct {
  hir_node_enum kind;
  span         *span;
} hir_base;

void hir_base_init(hir_base *self, span *span, hir_node_enum kind);
void hir_base_deinit(hir_base *self);

void hir_node_free(hir_base *generic);

// ID
typedef struct hir_id_struct {
  hir_base base;
  char    *name;
} hir_id;

typedef struct hir_id_data_struct {
  span *span;
  char *name;
} hir_id_data;

hir_id     *hir_id_new(span *span, char *name);
hir_id     *hir_id_copy(const hir_id *id);
hir_id_data hir_id_pop(hir_id *id);
void        hir_id_free(hir_id *id);

static inline void container_delete_hir_id(void *data) { hir_id_free(data); }
LIST_DECLARE_STATIC_INLINE(list_hir_id, hir_id, container_cmp_false,
                           container_new_move, container_delete_hir_id);

// LITERAL
typedef union hir_lit_u_union {
  long  v_long;
  ulong v_ulong;
  char *v_str;
  char *v_char;
  int   v_bool;
} hir_lit_u;

typedef struct hir_lit_struct {
  hir_base       base;
  hir_type_base *type;
  hir_lit_u      value;
} hir_lit;

hir_lit *hir_lit_new(span *span, hir_type_base *type, hir_lit_u value);
hir_lit *hir_lit_copy(const hir_lit *self);
void     hir_lit_free(hir_lit *self);

// PARAM
typedef struct hir_param {
  hir_base       base;
  hir_id        *id;
  hir_type_base *type;
  hir_symbol    *binding_ref;
} hir_param;

hir_param *hir_param_new(span *span, hir_id *id, hir_type_base *type,
                         hir_symbol *binding_ref);
void       hir_param_free(hir_param *self);

static inline void container_delete_hir_param(void *data) {
  hir_param_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_param, hir_param, container_cmp_false,
                           container_new_move, container_delete_hir_param);

// VAR
typedef struct hir_var {
  hir_base       base;
  hir_id        *id;
  hir_type_base *type;
  hir_symbol    *binding_ref;
} hir_var;

hir_var *hir_var_new(span *span, hir_id *id, hir_type_base *type,
                     hir_symbol *binding_ref);
void     hir_var_free(hir_var *self);

static inline void container_delete_hir_var(void *data) {
  hir_param_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_var, hir_var, container_cmp_false,
                           container_new_move, container_delete_hir_var);
