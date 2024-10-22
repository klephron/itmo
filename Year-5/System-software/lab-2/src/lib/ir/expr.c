#include "ir/expr.h"

#include "util/container_util.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdlib.h>

static void ir_expr_base_free_in(ir_expr_base *base) { UNUSED(base); }

static void ir_expr_unary_free(ir_expr_unary *self) {
  if (self) {
    ir_expr_base_free_in(&self->base);
    ir_expr_free(self->first);
    free(self);
  }
}

static void ir_expr_binary_free(ir_expr_binary *self) {
  if (self) {
    ir_expr_base_free_in(&self->base);
    ir_expr_free(self->first);
    ir_expr_free(self->second);
    free(self);
  }
}

static void ir_expr_lit_free(ir_expr_lit *self) {
  if (self) {
    ir_expr_base_free_in(&self->base);
    ir_term_lit_free(self->lit);
    free(self);
  }
}

static void ir_expr_id_free(ir_expr_id *self) {
  if (self) {
    ir_expr_base_free_in(&self->base);
    // TODO: stop freeing after reference implemented
    ir_term_id_free((void *)self->id);
    free(self);
  }
}

static void ir_expr_call_free(ir_expr_call *self) {
  if (self) {
    ir_expr_base_free_in(&self->base);
    ir_expr_free(self->callee);
    list_free(self->args);
    free(self);
  }
}

static void ir_expr_index_free(ir_expr_index *self) {
  if (self) {
    ir_expr_base_free_in(&self->base);
    ir_expr_free(self->indexed);
    list_free(self->args);
    free(self);
  }
}

void ir_expr_free(ir_expr_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case IR_EXPR_UNARY:
      return ir_expr_unary_free((ir_expr_unary *)generic);
    case IR_EXPR_BINARY:
      return ir_expr_binary_free((ir_expr_binary *)generic);
    case IR_EXPR_LITERAL:
      return ir_expr_lit_free((ir_expr_lit *)generic);
    case IR_EXPR_IDENTIFIER:
      return ir_expr_id_free((ir_expr_id *)generic);
    case IR_EXPR_CALL:
      return ir_expr_call_free((ir_expr_call *)generic);
    case IR_EXPR_INDEX:
      return ir_expr_index_free((ir_expr_index *)generic);
    default:
      error("unknown expression is freed");
      return ir_expr_base_free_in(generic);
    }
  }
}

static void ir_expr_base_init(ir_expr_base *base, ir_expr_enum kind) {
  base->kind = kind;
  base->type = IR_T_UNKNOWN;
}

static void ir_expr_unary_init(ir_expr_unary *unary, ir_expr_unary_enum op) {
  unary->op = op;
  ir_expr_base_init(&unary->base, IR_EXPR_UNARY);
}

static void ir_expr_binary_init(ir_expr_binary     *binary,
                                ir_expr_binary_enum op) {
  binary->op = op;
  ir_expr_base_init(&binary->base, IR_EXPR_BINARY);
}

ir_expr_unary *ir_expr_unary_new(ir_expr_base *first, ir_expr_unary_enum op) {
  ir_expr_unary *self = MALLOC(ir_expr_unary);
  ir_expr_unary_init(self, op);
  /*debug("created expr_unary op=%d", op);*/
  self->first = first;
  return self;
}

ir_expr_binary *ir_expr_binary_new(ir_expr_base *first, ir_expr_base *second,
                                   ir_expr_binary_enum op) {
  ir_expr_binary *self = MALLOC(ir_expr_binary);
  ir_expr_binary_init(self, op);
  self->first = first;
  self->second = second;
  /*debug("created expr_binary op=%d", op);*/
  return self;
}

ir_expr_lit *ir_expr_lit_new(ir_term_lit *lit) {
  ir_expr_lit *self = MALLOC(ir_expr_lit);
  ir_expr_base_init(&self->base, IR_EXPR_LITERAL);
  self->lit = lit;
  /*debug("created expr_lit");*/
  return self;
}

ir_expr_id *ir_expr_id_new(const ir_term_id *id) {
  ir_expr_id *self = MALLOC(ir_expr_id);
  ir_expr_base_init(&self->base, IR_EXPR_IDENTIFIER);
  self->id = id;
  /*debug("created expr_id");*/
  return self;
}

ir_expr_call *ir_expr_call_new(ir_expr_base *callee, p_list args) {
  ir_expr_call *self = MALLOC(ir_expr_call);
  ir_expr_base_init(&self->base, IR_EXPR_CALL);
  self->callee = callee;
  self->args = args;
  /*debug("created expr_call");*/
  return self;
}

ir_expr_index *ir_expr_index_new(ir_expr_base *indexed, p_list args) {
  ir_expr_index *self = MALLOC(ir_expr_index);
  ir_expr_base_init(&self->base, IR_EXPR_INDEX);
  self->indexed = indexed;
  self->args = args;
  /*debug("created expr_index");*/
  return self;
}

static void container_delete_ir_param(void *data) {
  ir_expr_base *self = data;
  ir_expr_free(self);
}

p_list ir_expr_list_new() {
  /*debug("created expr_list");*/
  p_list self = list_new(container_cmp_false, container_new_move,
                         container_delete_ir_param);
  return self;
}

const char *ir_expr_unary_ttstr(ir_expr_unary_enum type) {
  switch (type) {
  case IR_EXPR_UNARY_PLUS:
    return "PLUS";
  case IR_EXPR_UNARY_MINUS:
    return "MINUS";
  case IR_EXPR_UNARY_LOGICAL_NOT:
    return "LOGICAL_NOT";
  case IR_EXPR_UNARY_BITWISE_NOT:
    return "BITWISE_NOT";
  case IR_EXPR_UNARY_LOAD:
    return "LOAD";
  case IR_EXPR_UNARY_INC:
    return "INC";
  case IR_EXPR_UNARY_DEC:
    return "DEC";
  default:
    return "UNKNOWN";
  }
}

const char *ir_expr_binary_ttstr(ir_expr_binary_enum type) {
  switch (type) {
  case IR_EXPR_BINARY_ASSIGN:
    return "ASSIGN";
  case IR_EXPR_BINARY_LOGICAL_OR:
    return "LOGICAL_OR";
  case IR_EXPR_BINARY_LOGICAL_AND:
    return "LOGICAL_AND";
  case IR_EXPR_BINARY_BITWISE_OR:
    return "BITWISE_OR";
  case IR_EXPR_BINARY_BITWISE_XOR:
    return "BITWISE_XOR";
  case IR_EXPR_BINARY_BITWISE_AND:
    return "BITWISE_AND";
  case IR_EXPR_BINARY_EQUALS:
    return "EQUALS";
  case IR_EXPR_BINARY_NOT_EQUALS:
    return "NOT_EQUALS";
  case IR_EXPR_BINARY_LESS:
    return "LESS";
  case IR_EXPR_BINARY_LESS_EQUALS:
    return "LESS_EQUALS";
  case IR_EXPR_BINARY_GREATER:
    return "GREATER";
  case IR_EXPR_BINARY_GREATER_EQUALS:
    return "GREATER_EQUALS";
  case IR_EXPR_BINARY_BITWISE_SHIFT_LEFT:
    return "BITWISE_SHIFT_LEFT";
  case IR_EXPR_BINARY_BITWISE_SHIFT_RIGHT:
    return "BITWISE_SHIFT_RIGHT";
  case IR_EXPR_BINARY_ADD:
    return "ADD";
  case IR_EXPR_BINARY_SUB:
    return "SUB";
  case IR_EXPR_BINARY_MUL:
    return "MUL";
  case IR_EXPR_BINARY_DIV:
    return "DIV";
  case IR_EXPR_BINARY_MOD:
    return "MOD";
  default:
    return "UNKNOWN";
  }
}
