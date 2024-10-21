#include "ir/block.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdlib.h>

static void ir_block_base_init(ir_block_base *self, ir_block_enum kind) {
  self->kind = kind;
  self->status = IR_BLOCK_STATUS_OK;
}

void ir_block_base_free_in(ir_block_base *self) { UNUSED(self); }

void ir_block_cond_free(ir_block_cond *self) {
  if (self) {
    ir_block_base_free_in(&self->base);
    ir_block_free(self->je);
    ir_block_free(self->jz);
    ir_expr_free(self->cond);
  }
}

void ir_block_next_free(ir_block_next *self) {
  if (self) {
    ir_block_base_free_in(&self->base);
    ir_block_free(self->next);
    ir_expr_free(self->expr);
  }
}

void ir_block_free(ir_block_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case IR_BLOCK_NEXT:
      return ir_block_next_free((ir_block_next *)generic);
    case IR_BLOCK_COND:
      return ir_block_cond_free((ir_block_cond *)generic);
    case IR_BLOCK_UNKNOWN:
    default:
      error("unknown block type");
      return ir_block_base_free_in(generic);
    }
  }
}

ir_block_next *ir_block_next_new(ir_block_base *next, ir_expr_base *expr) {
  ir_block_next *self = MALLOC(ir_block_next);
  ir_block_base_init(&self->base, IR_BLOCK_NEXT);
  self->next = next;
  self->expr = expr;
  return self;
}

ir_block_cond *ir_block_cond_new(ir_block_base *jz, ir_block_base *je,
                                 ir_expr_base *cond) {
  ir_block_cond *self = MALLOC(ir_block_cond);
  ir_block_base_init(&self->base, IR_BLOCK_COND);
  self->jz = jz;
  self->je = je;
  self->cond = cond;
  return self;
}

void ir_block_graph_free(ir_block_base *root) {
  debug("free block graph");
  if (root) {
  }
}
