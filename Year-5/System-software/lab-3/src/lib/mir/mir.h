#pragma once

#include "hir/node.h"
#include "type/type.h"

/*
 * Linearized representation of eir (used mostly for lab purpose)
 * with(out) SSA and some optimizations.
 *
 * Looks like this:
 *
 * `value` has fixed size, represents dynamic variable
 * can store type_id, data_size, data_ptr
 *
 * fn main(): ret_type {
 *   VALUE(_0, ret_type);
 *   VALUE(_1, unknown);
 *   ... other variables
 *   VALUE(_3: bool);
 *
 *   // example implementation of VALUE (declaration)
 *   // with ret_type specified as int_64
 *   sub rsp, 32
 *   mov [rbp - 8], 1      // type_id is 1, e.g., for int
 *   mov [rbp - 16], 8     // data_size is 8 bytes
 *   mov rax, rbp - 32     // Calculate rbp - 32 and store it in rax
 *   mov [rbp - 24], rax   // Store the value of rax into memory at [rbp - 24]
 *
 *   bb0: {
 *     // can work with dynamic values and constants
 *     STORE(_1, 6_i32);
 *     STORE(_3, EQUALS(_2, _3))
 *
 *     JMP(_3, bb1, bb0); // jump if _3 is resolved as True
 *   }
 *
 *   bb1: {
 *     ...code
 *   }
 * }
 */

// GENERIC
typedef enum mir_node_enum {
  MIR_NODE_UNKNOWN,
  MIR_NODE_VALUE,
  MIR_NODE_LIT,
  MIR_NODE_BUILTIN,
  MIR_NODE_SUBROUTINE,
  MIR_NODE_SUBROUTINE_EXTERNAL,
} mir_node_enum;

typedef struct mir_node_base_struct {
  mir_node_enum kind;
} mir_node_base;

void mir_node_free(mir_node_base *generic);

static inline void container_delete_mir_node(void *data) {
  return mir_node_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_node, mir_node_base, container_cmp_false,
                           container_new_move, container_delete_mir_node);

// VALUE
typedef struct mir_value_struct {
  mir_node_base base;
  size_t        id;
  type_base    *type;
} mir_value;

mir_value *mir_value_new(size_t id, type_base *type);
void       mir_value_free(mir_value *self);

static inline void container_delete_mir_value(void *data) {
  return mir_value_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_value, mir_value, container_cmp_false,
                           container_new_move, container_delete_mir_value);

// ARGS
void *container_new_mir_arg(void *);
void  container_delete_mir_arg(void *);
LIST_DECLARE_STATIC_INLINE(list_mir_arg, mir_node_base, container_cmp_false,
                           container_new_mir_arg, container_delete_mir_arg);

// LIT
typedef struct mir_lit_struct {
  mir_node_base base;
  hir_lit      *lit;
} mir_lit;

mir_lit *mir_lit_new(hir_lit *lit);
void     mir_lit_free(mir_lit *self);

// BUILTIN
typedef enum mir_builtin_enum {
  MIR_BUILTIN_UNKNOWN,
  MIR_BUILTIN_STORE,
  MIR_BUILTIN_PLUS,
  MIR_BUILTIN_MINUS,
  MIR_BUILTIN_LOGICAL_NOT,
  MIR_BUILTIN_BITWISE_NOT,
  MIR_BUILTIN_INC,
  MIR_BUILTIN_DEC,
  MIR_BUILTIN_LOAD,
  MIR_BUILTIN_LOGICAL_OR,
  MIR_BUILTIN_LOGICAL_AND,
  MIR_BUILTIN_BITWISE_OR,
  MIR_BUILTIN_BITWISE_XOR,
  MIR_BUILTIN_BITWISE_AND,
  MIR_BUILTIN_EQUALS,
  MIR_BUILTIN_NOT_EQUALS,
  MIR_BUILTIN_LESS,
  MIR_BUILTIN_LESS_EQUALS,
  MIR_BUILTIN_GREATER,
  MIR_BUILTIN_GREATER_EQUALS,
  MIR_BUILTIN_BITWISE_SHIFT_LEFT,
  MIR_BUILTIN_BITWISE_SHIFT_RIGHT,
  MIR_BUILTIN_ADD,
  MIR_BUILTIN_SUB,
  MIR_BUILTIN_MUL,
  MIR_BUILTIN_DIV,
  MIR_BUILTIN_MOD,
  MIR_BUILTIN_CALL,
  MIR_BUILTIN_INDEX,
  MIR_BUILTIN_STORE_INDEX,
  MIR_BUILTIN_CAST,
  MIR_BUILTIN_NEW,
  MIR_BUILTIN_FREE
} mir_builtin_enum;

typedef struct mir_builtin_struct {
  mir_node_base    base;
  mir_builtin_enum kind;
  list_mir_arg    *args;
  type_base       *spec;
} mir_builtin;

mir_builtin *mir_builtin_new(mir_builtin_enum kind, list_mir_arg *args,
                             type_base *spec);
void         mir_builtin_free(mir_builtin *self);

// CALL
typedef struct mir_call_struct {
  mir_node_base        base;
  const mir_node_base *node_ref; // subroutine/subroutine_ext/value_ref
  list_mir_arg        *args;
} mir_call;

mir_call *mir_call_new(const mir_node_base *node_ref, list_mir_arg *args);
void      mir_call_free(mir_call *self);

// JUMPS (inside basic blocks)
typedef struct mir_bb_struct mir_bb;

// if value == nil then jmp to jz
typedef struct mir_jmp_struct {
  const mir_value *value_ref;
  const mir_bb    *je_ref;
  const mir_bb    *jz_ref;
} mir_jmp;

mir_jmp *mir_jmp_new(const mir_value *value_ref, const mir_bb *je_ref,
                     const mir_bb *jz_ref);
void     mir_jmp_free(mir_jmp *self);

// BASIC BLOCK
// if jmp is nil then return
typedef struct mir_bb_struct {
  size_t         id;
  list_mir_node *stmts;
  mir_jmp       *jmp;
} mir_bb;

mir_bb *mir_bb_new(size_t id, list_mir_node *stmts, mir_jmp *jmp);
void    mir_bb_free(mir_bb *self);

static inline void container_delete_mir_bb(void *data) { mir_bb_free(data); }
LIST_DECLARE_STATIC_INLINE(list_mir_bb, mir_bb, container_cmp_false,
                           container_new_move, container_delete_mir_bb);

// SUBROUTINE
typedef struct mir_subroutine_struct {
  mir_node_base   base;
  char           *name;
  mir_value      *ret; // id=0
  list_mir_value *args;
  list_mir_value *values;
  list_mir_bb    *bbs;
  const mir_bb   *bb_first;
} mir_subroutine;

mir_subroutine *mir_subroutine_new(char *name, mir_value *ret,
                                   list_mir_value *args, list_mir_value *values,
                                   list_mir_bb *bbs);
void            mir_subroutine_free(mir_subroutine *self);

static inline void container_delete_mir_subroutine(void *data) {
  mir_subroutine_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_subroutine, mir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_mir_subroutine);

// SUBROUTINE EXTERNAL
typedef struct mir_subroutine_ext_struct {
  mir_node_base base;
  char         *name;
  type_base    *ret;
  list_type    *args;
} mir_subroutine_ext;

mir_subroutine_ext *mir_subroutine_ext_new(char *name, type_base *ret,
                                           list_type *args);
void                mir_subroutine_ext_free(mir_subroutine_ext *subroutine);

static inline void container_delete_mir_subroutine_ext(void *data) {
  mir_subroutine_ext_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_subroutine_ext, mir_subroutine_ext,
                           container_cmp_false, container_new_move,
                           container_delete_mir_subroutine_ext);

typedef struct mir_struct {
  list_mir_subroutine_ext *sub_exts;
  list_mir_subroutine     *subs;
} mir;

mir *mir_new();
void mir_free(mir *self);
