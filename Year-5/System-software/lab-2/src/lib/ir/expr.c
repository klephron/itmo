#include "ir/expr.h"

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

static void ir_expr_base_init(ir_expr_base *base, ir_expr_enum kind) {
  base->kind = kind;
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
  debug("created expr_unary op=%d", op);
  self->first = first;
  return self;
}

ir_expr_binary *ir_expr_binary_new(ir_expr_base *first, ir_expr_base *second,
                                   ir_expr_binary_enum op) {
  ir_expr_binary *self = MALLOC(ir_expr_binary);
  ir_expr_binary_init(self, op);
  self->first = first;
  self->second = second;
  debug("created expr_binary op=%d", op);
  return self;
}

ir_expr_lit *ir_expr_lit_new(ir_term_lit *lit) {
  ir_expr_lit *self = MALLOC(ir_expr_lit);
  ir_expr_base_init(&self->base, IR_EXPR_LITERAL);
  self->lit = lit;
  debug("created expr_lit");
  return self;
}

ir_expr_id *ir_expr_id_new(const ir_term_id *id) {
  ir_expr_id *self = MALLOC(ir_expr_id);
  ir_expr_base_init(&self->base, IR_EXPR_IDENTIFIER);
  self->id = id;
  debug("created expr_id");
  return self;
}

ir_expr_call *ir_expr_call_new(ir_expr_base *callee, p_list args) {
  ir_expr_call *self = MALLOC(ir_expr_call);
  ir_expr_base_init(&self->base, IR_EXPR_CALL);
  self->callee = callee;
  self->args = args;
  debug("created expr_call");
  return self;
}

ir_expr_index *ir_expr_index_new(ir_expr_base *indexed, p_list args) {
  ir_expr_index *self = MALLOC(ir_expr_index);
  ir_expr_base_init(&self->base, IR_EXPR_INDEX);
  self->indexed = indexed;
  self->args = args;
  debug("created expr_index");
  return self;
}
