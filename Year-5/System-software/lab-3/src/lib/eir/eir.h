#pragma once

#include "hir/node.h"
#include "hir/symbols.h"
#include "util/container_util.h"

// EXPR
typedef enum eir_expr_enum {
  EIR_EXPR_UNKNOWN,
  EIR_EXPR_UNARY,
  EIR_EXPR_BINARY,
  EIR_EXPR_LITERAL,
  EIR_EXPR_IDENTIFIER,
  EIR_EXPR_CALL,
  EIR_EXPR_INDEX,
} eir_expr_enum;

typedef enum eir_expr_unary_enum {
  EIR_EXPR_UNARY_UNKNOWN,
  EIR_EXPR_UNARY_PLUS,
  EIR_EXPR_UNARY_MINUS,
  EIR_EXPR_UNARY_LOGICAL_NOT,
  EIR_EXPR_UNARY_BITWISE_NOT,
  EIR_EXPR_UNARY_INC,
  EIR_EXPR_UNARY_DEC,
  EIR_EXPR_UNARY_LOAD
} eir_expr_unary_enum;

typedef enum eir_expr_binary_enum {
  EIR_EXPR_BINARY_UNKNOWN,
  EIR_EXPR_BINARY_STORE,
  EIR_EXPR_BINARY_LOGICAL_OR,
  EIR_EXPR_BINARY_LOGICAL_AND,
  EIR_EXPR_BINARY_BITWISE_OR,
  EIR_EXPR_BINARY_BITWISE_XOR,
  EIR_EXPR_BINARY_BITWISE_AND,
  EIR_EXPR_BINARY_EQUALS,
  EIR_EXPR_BINARY_NOT_EQUALS,
  EIR_EXPR_BINARY_LESS,
  EIR_EXPR_BINARY_LESS_EQUALS,
  EIR_EXPR_BINARY_GREATER,
  EIR_EXPR_BINARY_GREATER_EQUALS,
  EIR_EXPR_BINARY_BITWISE_SHIFT_LEFT,
  EIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT,
  EIR_EXPR_BINARY_ADD,
  EIR_EXPR_BINARY_SUB,
  EIR_EXPR_BINARY_MUL,
  EIR_EXPR_BINARY_DIV,
  EIR_EXPR_BINARY_MOD,
} eir_expr_binary_enum;

typedef struct eir_expr_base_struct {
  eir_expr_enum kind;
  type_base    *type;
  span         *span;
} eir_expr_base;

void eir_expr_free(eir_expr_base *generic);

static inline void container_delete_eir_expr(void *data) {
  eir_expr_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_eir_expr, eir_expr_base, container_cmp_false,
                           container_new_move, container_delete_eir_expr);

typedef struct eir_expr_unary_struct {
  eir_expr_base       base;
  eir_expr_unary_enum op;
  eir_expr_base      *first;
} eir_expr_unary;

eir_expr_unary *eir_expr_unary_new(span *span, type_base *type,
                                   eir_expr_unary_enum op,
                                   eir_expr_base      *first);
void            eir_expr_unary_free(eir_expr_unary *self);

typedef struct eir_expr_binary_struct {
  eir_expr_base        base;
  eir_expr_binary_enum op;
  eir_expr_base       *first;
  eir_expr_base       *second;
} eir_expr_binary;

eir_expr_binary *eir_expr_binary_new(span *span, type_base *type,
                                     eir_expr_binary_enum op,
                                     eir_expr_base       *first,
                                     eir_expr_base       *second);
void             eir_expr_binary_free(eir_expr_binary *self);

typedef struct eir_expr_lit_struct {
  eir_expr_base base;
  hir_lit      *lit;
} eir_expr_lit;

eir_expr_lit *eir_expr_lit_new(span *span, type_base *type, hir_lit *lit);
void          eir_expr_lit_free(eir_expr_lit *self);

typedef struct eir_expr_id_struct {
  eir_expr_base base;
  hir_id       *id;
} eir_expr_id;

eir_expr_id *eir_expr_id_new(span *span, type_base *type, hir_id *id);
void         eir_expr_id_free(eir_expr_id *self);

typedef struct eir_expr_call_struct {
  eir_expr_base  base;
  eir_expr_base *callee;
  list_eir_expr *args;
} eir_expr_call;

eir_expr_call *eir_expr_call_new(span *span, type_base *type,
                                 eir_expr_base *callee, list_eir_expr *args);
void           eir_expr_call_free(eir_expr_call *self);

typedef struct eir_expr_index_struct {
  eir_expr_base  base;
  eir_expr_base *indexed;
  list_eir_expr *args;
} eir_expr_index;

eir_expr_index *eir_expr_index_new(span *span, type_base *type,
                                   eir_expr_base *indexed, list_eir_expr *args);
void            eir_expr_index_free(eir_expr_index *self);

// BLOCK
typedef enum eir_block_enum {
  EIR_BLOCK_UNKNOWN,
  EIR_BLOCK_NEXT,
  EIR_BLOCK_COND,
  EIR_BLOCK_UNTIED = 0b100,
} eir_block_enum;

typedef struct eir_block_base_struct {
  eir_block_enum kind;
  span          *span;
} eir_block_base;

void            eir_block_free(eir_block_base *generic);
void            eir_block_set_next(eir_block_base *self, eir_block_base *next);
eir_block_base *eir_block_get_next(eir_block_base *self);

typedef struct eir_block_next_struct {
  eir_block_base  base;
  eir_expr_base  *expr;
  eir_block_base *next;
} eir_block_next;

eir_block_next *eir_block_next_new(span *span, eir_expr_base *expr,
                                   eir_block_base *next);
void            eir_block_next_free(eir_block_next *self);

typedef struct eir_block_cond_struct {
  eir_block_base  base;
  eir_expr_base  *cond;
  eir_block_base *je;
  eir_block_base *jz;
} eir_block_cond;

eir_block_cond *eir_block_cond_new(span *span, eir_expr_base *cond,
                                   eir_block_base *je, eir_block_base *jz);
void            eir_block_cond_free(eir_block_cond *self);

// SUBROUTINE
typedef struct eir_subroutine_struct {
  eir_block_base   *root;
  const hir_symbol *binding;
} eir_subroutine;

eir_subroutine *eir_subroutine_new(eir_block_base   *root,
                                   const hir_symbol *binding);
void            eir_subroutine_free(eir_subroutine *self);

static inline void container_delete_eir_subroutine(void *data) {
  eir_subroutine_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_eir_subroutine, eir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_eir_subroutine);

// EIR
typedef struct eir_struct {
  const list_hir_symbol *globals;
  const list_hir_symbol *locals;
  list_eir_subroutine   *subroutines;
} eir;

eir *eir_new();
void eir_free(eir *self);
