#pragma once

#include "ir/loc.h"
#include "ir/terminal.h"
#include "util/list.h"

enum ir_expr_enum {
  IR_EXPR_UNKNOWN,
  IR_EXPR_UNARY,
  IR_EXPR_BINARY,
  IR_EXPR_LITERAL,
  IR_EXPR_IDENTIFIER,
  IR_EXPR_CALL,
  IR_EXPR_INDEX,
};

enum ir_expr_unary_enum {
  IR_EXPR_UNARY_UNKNOWN,
  IR_EXPR_UNARY_PLUS,
  IR_EXPR_UNARY_MINUS,
  IR_EXPR_UNARY_LOGICAL_NOT,
  IR_EXPR_UNARY_BITWISE_NOT,
  IR_EXPR_UNARY_LOAD,
  IR_EXPR_UNARY_INC,
  IR_EXPR_UNARY_DEC
};

enum ir_expr_binary_enum {
  IR_EXPR_BINARY_UNKNOWN,
  IR_EXPR_BINARY_ASSIGN,
  IR_EXPR_BINARY_LOGICAL_OR,
  IR_EXPR_BINARY_LOGICAL_AND,
  IR_EXPR_BINARY_BITWISE_OR,
  IR_EXPR_BINARY_BITWISE_XOR,
  IR_EXPR_BINARY_BITWISE_AND,
  IR_EXPR_BINARY_EQUALS,
  IR_EXPR_BINARY_NOT_EQUALS,
  IR_EXPR_BINARY_LESS,
  IR_EXPR_BINARY_LESS_EQUALS,
  IR_EXPR_BINARY_GREATER,
  IR_EXPR_BINARY_GREATER_EQUALS,
  IR_EXPR_BINARY_BITWISE_SHIFT_LEFT,
  IR_EXPR_BINARY_BITWISE_SHIFT_RIGHT,
  IR_EXPR_BINARY_ADD,
  IR_EXPR_BINARY_SUB,
  IR_EXPR_BINARY_MUL,
  IR_EXPR_BINARY_DIV,
  IR_EXPR_BINARY_MOD,
};

struct ir_expr_base {
  enum ir_expr_enum kind;
  enum ir_t_enum    type;
};

struct ir_expr_unary {
  struct ir_expr_base     base;
  enum ir_expr_unary_enum op;
  ir_loc                  op_loc;
  struct ir_expr_base    *first;
};

struct ir_expr_binary {
  struct ir_expr_base      base;
  enum ir_expr_binary_enum op;
  ir_loc                   op_loc;
  struct ir_expr_base     *first;
  struct ir_expr_base     *second;
};

struct ir_expr_lit {
  struct ir_expr_base base;
  struct ir_term_lit *lit;
};

struct ir_expr_id {
  struct ir_expr_base      base;
  const struct ir_term_id *id; // references variable
};

struct ir_expr_call {
  struct ir_expr_base  base;
  struct ir_expr_base *callee;
  p_list               args;
};

struct ir_expr_index {
  struct ir_expr_base  base;
  struct ir_expr_base *indexed;
  p_list               args;
};

typedef enum ir_expr_enum        ir_expr_enum;
typedef enum ir_expr_unary_enum  ir_expr_unary_enum;
typedef enum ir_expr_binary_enum ir_expr_binary_enum;
typedef enum ir_expr_id_enum     ir_expr_id_enum;
typedef struct ir_expr_base      ir_expr_base;
typedef struct ir_expr_unary     ir_expr_unary;
typedef struct ir_expr_binary    ir_expr_binary;
typedef struct ir_expr_lit       ir_expr_lit;
typedef struct ir_expr_id        ir_expr_id;
typedef struct ir_expr_call      ir_expr_call;
typedef struct ir_expr_index     ir_expr_index;

ir_expr_unary  *ir_expr_unary_new(ir_expr_base           *first,
                                  enum ir_expr_unary_enum op, ir_loc op_loc);
ir_expr_binary *ir_expr_binary_new(ir_expr_base *first, ir_expr_base *second,
                                   enum ir_expr_binary_enum op, ir_loc op_loc);
ir_expr_lit    *ir_expr_lit_new(ir_term_lit *lit);
ir_expr_id     *ir_expr_id_new(const ir_term_id *id);
ir_expr_call   *ir_expr_call_new(ir_expr_base *callee, p_list args);
ir_expr_index  *ir_expr_index_new(ir_expr_base *indexed, p_list args);

p_list ir_expr_list_new();

const char *ir_expr_unary_ttstr(ir_expr_unary_enum type);
const char *ir_expr_binary_ttstr(ir_expr_binary_enum type);

void          ir_expr_free(ir_expr_base *generic);
ir_expr_base *ir_expr_copy(ir_expr_base *generic);
