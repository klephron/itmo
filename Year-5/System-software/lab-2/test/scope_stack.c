#include "ir/scope.h"
#include "util/log.h"
#include <criterion/criterion.h>

Test(list, push_front) {
  scope_stack stack = ir_scope_stack_new();

  ir_block_base *begin = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_block_base *end = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_block_base *cur = begin;

  ir_scope_stack_push(stack, IR_SCOPE_FUNCTION, begin, end);
  ir_scope *root = ir_scope_stack_top(stack);
  root->prev = cur;

  ir_scope *parent = ir_scope_stack_top(stack);

  debug("current scope: %p, begin=%p, end=%p, prev=%p", root, root->begin,
        root->end, root->prev);
  debug("parent scope: %p, begin=%p, end=%p, prev=%p", parent, parent->begin,
        parent->end, parent->prev);

  ir_scope_stack_free(stack);
}
