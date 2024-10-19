#include "ir/block.h"
#include "util/macro.h"
#include <stdlib.h>

static void ir_block_base_init(ir_block_base *self, ir_block_enum kind) {
  self->kind = kind;
}

ir_block_next *ir_block_next_new() {
  ir_block_next *self = MALLOC(ir_block_next);
  ir_block_base_init(&self->base, IR_BLOCK_NEXT);
  self->next = NULL;
  self->expr = NULL;
  return self;
}

ir_block_cond *ir_block_cond_new() {
  ir_block_cond *self = MALLOC(ir_block_cond);
  ir_block_base_init(&self->base, IR_BLOCK_COND);
  self->jz = NULL;
  self->je = NULL;
  self->expr = NULL;
  return self;
}
