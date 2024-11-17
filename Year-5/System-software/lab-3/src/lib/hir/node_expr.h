#pragma once

#include "hir/node.h"

typedef enum hir_expr_enum {
  HIR_EXPR_UNKNOWN,
  HIR_EXPR_UNARY,
  HIR_EXPR_BINARY,
  HIR_EXPR_LITERAL,
  HIR_EXPR_IDENTIFIER,
  HIR_EXPR_CALL,
  HIR_EXPR_INDEX,
  HIR_EXPR_CAST,
} hir_expr_enum;

typedef enum hir_expr_unary_enum {
  HIR_EXPR_UNARY_UNKNOWN,
  HIR_EXPR_UNARY_PLUS,
  HIR_EXPR_UNARY_MINUS,
  HIR_EXPR_UNARY_LOGICAL_NOT,
  HIR_EXPR_UNARY_BITWISE_NOT,
  HIR_EXPR_UNARY_INC,
  HIR_EXPR_UNARY_DEC
} hir_expr_unary_enum;

typedef enum hir_expr_binary_enum {
  HIR_EXPR_BINARY_UNKNOWN,
  HIR_EXPR_BINARY_ASSIGN,
  HIR_EXPR_BINARY_LOGICAL_OR,
  HIR_EXPR_BINARY_LOGICAL_AND,
  HIR_EXPR_BINARY_BITWISE_OR,
  HIR_EXPR_BINARY_BITWISE_XOR,
  HIR_EXPR_BINARY_BITWISE_AND,
  HIR_EXPR_BINARY_EQUALS,
  HIR_EXPR_BINARY_NOT_EQUALS,
  HIR_EXPR_BINARY_LESS,
  HIR_EXPR_BINARY_LESS_EQUALS,
  HIR_EXPR_BINARY_GREATER,
  HIR_EXPR_BINARY_GREATER_EQUALS,
  HIR_EXPR_BINARY_BITWISE_SHIFT_LEFT,
  HIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT,
  HIR_EXPR_BINARY_ADD,
  HIR_EXPR_BINARY_SUB,
  HIR_EXPR_BINARY_MUL,
  HIR_EXPR_BINARY_DIV,
  HIR_EXPR_BINARY_MOD,
} hir_expr_binary_enum;

typedef struct hir_expr_base_struct {
  hir_base      base;
  hir_expr_enum kind;
  type_base    *type;
} hir_expr_base;

void hir_expr_free(hir_expr_base *generic);

static inline void container_delete_hir_expr(void *data) {
  hir_expr_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_expr, hir_expr_base, container_cmp_false,
                           container_new_move, container_delete_hir_expr);

typedef struct hir_expr_unary_struct {
  hir_expr_base       base;
  hir_expr_unary_enum op;
  hir_expr_base      *first;
} hir_expr_unary;

hir_expr_unary *hir_expr_unary_new(span *span, type_base *type,
                                   hir_expr_unary_enum op,
                                   hir_expr_base      *first);
void            hir_expr_unary_free(hir_expr_unary *self);

typedef struct hir_expr_binary_struct {
  hir_expr_base        base;
  hir_expr_binary_enum op;
  hir_expr_base       *first;
  hir_expr_base       *second;
} hir_expr_binary;

hir_expr_binary *hir_expr_binary_new(span *span, type_base *type,
                                     hir_expr_binary_enum op,
                                     hir_expr_base       *first,
                                     hir_expr_base       *second);
void             hir_expr_binary_free(hir_expr_binary *self);

typedef struct hir_expr_lit_struct {
  hir_expr_base base;
  hir_lit      *lit;
} hir_expr_lit;

hir_expr_lit *hir_expr_lit_new(span *span, type_base *type, hir_lit *lit);
void          hir_expr_lit_free(hir_expr_lit *self);

typedef struct hir_expr_id {
  hir_expr_base     base;
  hir_id           *id;
  const hir_symbol *binding;
} hir_expr_id;

hir_expr_id *hir_expr_id_new(span *span, type_base *type, hir_id *id,
                             const hir_symbol *binding);
void         hir_expr_id_free(hir_expr_id *self);

typedef struct hir_expr_call {
  hir_expr_base  base;
  hir_expr_base *callee;
  list_hir_expr *args;
} hir_expr_call;

hir_expr_call *hir_expr_call_new(span *span, type_base *type,
                                 hir_expr_base *callee, list_hir_expr *args);
void           hir_expr_call_free(hir_expr_call *self);

typedef struct hir_expr_index {
  hir_expr_base  base;
  hir_expr_base *indexed;
  list_hir_expr *args;
} hir_expr_index;

hir_expr_index *hir_expr_index_new(span *span, type_base *type,
                                   hir_expr_base *indexed, list_hir_expr *args);
void            hir_expr_index_free(hir_expr_index *self);

typedef struct hir_expr_cast {
  hir_expr_base  base;
  hir_expr_base *expr;
} hir_expr_cast;

hir_expr_cast *hir_expr_cast_new(span *span, type_base *type,
                                 hir_expr_base *expr);
void           hir_expr_cast_free(hir_expr_cast *self);
