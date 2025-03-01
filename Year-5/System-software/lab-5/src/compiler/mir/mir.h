#pragma once

#include "compiler/hir/node_expr.h"
#include "compiler/symbol_table/symbol_table.h"

typedef struct mir_subroutine_struct     mir_subroutine;
typedef struct mir_subroutine_ext_struct mir_subroutine_ext;

LIST_DECLARE_STATIC_INLINE(list_hir_expr_ref, hir_expr_base,
                           container_cmp_false, container_new_move,
                           container_delete_false);

// VALUE
typedef struct mir_value_struct {
  size_t              id;
  const symbol_entry *symbol_ref;
  const type_entry   *type_ref;
} mir_value;

mir_value *mir_value_new(size_t id, const symbol_entry *symbol_ref,
                         const type_entry *type_ref);
void       mir_value_free(mir_value *self);

static inline void container_delete_mir_value(void *data) {
  return mir_value_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_value, mir_value, container_cmp_false,
                           container_new_move, container_delete_mir_value);

LIST_DECLARE_STATIC_INLINE(list_mir_value_ref, mir_value, container_cmp_false,
                           container_new_move, container_delete_false);

// LIT
typedef union mir_lit_value_union {
  uint8_t  v_bool;
  uint8_t  v_byte;
  int32_t  v_int;
  uint32_t v_uint;
  int64_t  v_long;
  uint64_t v_ulong;
  uint8_t  v_char;
  uint8_t *v_str;
} mir_lit_value;

typedef struct mir_lit_struct {
  size_t            id;
  const type_entry *type_ref;
  mir_lit_value     value;
} mir_lit;

mir_lit *mir_lit_new(size_t id, const type_entry *type_ref,
                     mir_lit_value value);
void     mir_lit_free(mir_lit *self);

// MIR_STMT
typedef enum mir_stmt_enum {
  MIR_STMT_UNKNOWN,
  MIR_STMT_OP,
  MIR_STMT_CALL,
  MIR_STMT_MEMBER,
  MIR_STMT_BUILTIN,
  MIR_STMT_ASSIGN,
} mir_stmt_enum;

typedef enum mir_stmt_op_enum {
  MIR_STMT_OP_UNKNOWN,
  MIR_STMT_OP_UNARY_PLUS,
  MIR_STMT_OP_UNARY_MINUS,
  MIR_STMT_OP_UNARY_LOGICAL_NOT,
  MIR_STMT_OP_UNARY_BITWISE_NOT,
  MIR_STMT_OP_UNARY_INC,
  MIR_STMT_OP_UNARY_DEC,
  MIR_STMT_OP_BINARY_LOGICAL_OR,
  MIR_STMT_OP_BINARY_LOGICAL_AND,
  MIR_STMT_OP_BINARY_BITWISE_OR,
  MIR_STMT_OP_BINARY_BITWISE_XOR,
  MIR_STMT_OP_BINARY_BITWISE_AND,
  MIR_STMT_OP_BINARY_EQUALS,
  MIR_STMT_OP_BINARY_NOT_EQUALS,
  MIR_STMT_OP_BINARY_LESS,
  MIR_STMT_OP_BINARY_LESS_EQUALS,
  MIR_STMT_OP_BINARY_GREATER,
  MIR_STMT_OP_BINARY_GREATER_EQUALS,
  MIR_STMT_OP_BINARY_BITWISE_SHIFT_LEFT,
  MIR_STMT_OP_BINARY_BITWISE_SHIFT_RIGHT,
  MIR_STMT_OP_BINARY_ADD,
  MIR_STMT_OP_BINARY_SUB,
  MIR_STMT_OP_BINARY_MUL,
  MIR_STMT_OP_BINARY_DIV,
  MIR_STMT_OP_BINARY_REM,
  MIR_STMT_OP_CALL_INDIRECT,
  MIR_STMT_OP_INDEX_INDIRECT,
} mir_stmt_op_enum;

typedef enum mir_stmt_builtin_enum {
  MIR_STMT_BUILTIN_UNKNOWN,
  MIR_STMT_BUILTIN_CAST,
  MIR_STMT_BUILTIN_MAKE,
  MIR_STMT_BUILTIN_PRINT,
} mir_stmt_builtin_enum;

// NOTE: references elements that are stored elsewhere
typedef struct mir_stmt_struct {
  mir_stmt_enum kind;
  union {
    struct {
      mir_stmt_op_enum    kind;
      mir_value          *ret;
      list_mir_value_ref *args;
    } op;
    struct {
      mir_value          *ret;
      mir_subroutine     *sub;
      list_mir_value_ref *args;
    } call;
    struct {
      mir_value *ret;
      mir_value *obj;
      mir_lit   *member;
    } member;
    struct {
      mir_stmt_builtin_enum kind;
      mir_value            *ret;
      type_entry           *type; // template
      list_mir_value_ref   *args;
    } builtin;
    struct {
      mir_value *to;
      mir_value *from;
    } assign;
  };
} mir_stmt;

mir_stmt *mir_stmt_new_op(mir_stmt_op_enum kind, mir_value *ret,
                          list_mir_value_ref *args);
mir_stmt *mir_stmt_new_call(mir_value *ret, mir_subroutine *sub,
                            list_mir_value_ref *args);
mir_stmt *mir_stmt_new_member(mir_value *ret, mir_value *obj, mir_lit *member);
mir_stmt *mir_stmt_new_builtin(mir_stmt_builtin_enum kind, mir_value *ret,
                               type_entry *type, list_mir_value_ref *args);
mir_stmt *mir_stmt_new_assign(mir_value *to, mir_value *from);

void mir_stmt_free(mir_stmt *self);

static inline void container_delete_mir_stmt(void *data) {
  mir_stmt_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_stmt, mir_stmt, container_cmp_false,
                           container_new_move, container_delete_mir_stmt);

// BB
typedef enum mir_bb_enum {
  MIR_BB_UNKNOWN,
  MIR_BB_COND,
  MIR_BB_NEXT,
  MIR_BB_TERM,
} mir_bb_enum;

typedef struct mir_bb_struct mir_bb;
struct mir_bb_struct {
  size_t         id;
  list_mir_stmt *stmts;
  struct { // terminator
    mir_value *cond_ref;
    mir_bb    *je_ref;
    union { // means the same
      mir_bb *jz_ref;
      mir_bb *next_ref;
    };
  } jmp;
  list_hir_expr_ref *hir_exprs;
};

mir_bb *mir_bb_new(size_t id, list_mir_stmt *stmts, mir_value *jmp_cond,
                   mir_bb *jmp_je_ref, mir_bb *jmp_jz_ref,
                   list_hir_expr_ref *hir_exprs);
void    mir_bb_free(mir_bb *self);

mir_bb_enum mir_bb_get_cond(const mir_bb *self);

static inline void container_delete_mir_bb(void *data) { mir_bb_free(data); }
LIST_DECLARE_STATIC_INLINE(list_mir_bb, mir_bb, container_cmp_false,
                           container_new_move, container_delete_mir_bb);

// SUBROUTINE
typedef enum mir_subroutine_enum {
  MIR_SUBROUTINE_UNKNOWN,
  MIR_SUBROUTINE_BASIC,
  MIR_SUBROUTINE_EXTERNAL,
  MIR_SUBROUTINE_IMPORTED
} mir_subroutine_enum;

struct mir_subroutine_struct {
  mir_subroutine_enum kind;
  const symbol_entry *symbol_ref;
  const type_entry   *type_ref;
  union {
    struct {
      mir_value      *ret;
      list_mir_value *params;
      list_mir_value *vars;
      list_mir_value *tmps;
      list_mir_bb    *bbs;
    } basic;
    struct {
    } external;
    struct {
      char *lib;
      char *entry;
    } imported;
  };
};

mir_subroutine *mir_subroutine_new_basic(const symbol_entry *symbol_ref,
                                         const type_entry   *type_ref,
                                         mir_value *ret, list_mir_value *params,
                                         list_mir_value *vars,
                                         list_mir_value *tmps,
                                         list_mir_bb    *bbs);
mir_subroutine *mir_subroutine_new_external(const symbol_entry *symbol_ref,
                                            const type_entry   *type_ref);
mir_subroutine *mir_subroutine_new_imported(const symbol_entry *symbol_ref,
                                            const type_entry   *type_ref,
                                            char *lib, char *entry);
void            mir_subroutine_free(mir_subroutine *self);

static inline void container_delete_mir_subroutine(void *data) {
  mir_subroutine_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_subroutine, mir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_mir_subroutine);

LIST_DECLARE_STATIC_INLINE(list_mir_subroutine_ref, mir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_false);

// CLASSES
typedef struct mir_class_struct {
  const type_entry        *type_ref;
  list_mir_value          *fields;
  list_mir_subroutine_ref *methods;
} mir_class;

mir_class *mir_class_new(const type_entry *type_ref, list_mir_value *fields,
                         list_mir_subroutine_ref *methods);
void       mir_class_free(mir_class *self);

static inline void container_delete_mir_class(void *data) {
  mir_class_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_mir_class, mir_class, container_cmp_false,
                           container_new_move, container_delete_mir_class);

// MIR
typedef struct mir_struct {
  list_mir_subroutine *global_subs;
  list_mir_subroutine *external_subs;
  list_mir_subroutine *imported_subs;
  list_mir_subroutine *methods;
  list_mir_class      *classes;
} mir;

mir *mir_new();
void mir_free(mir *self);
