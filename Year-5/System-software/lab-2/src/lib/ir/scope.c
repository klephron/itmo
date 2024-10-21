#include "ir/scope.h"
#include "util/container_util.h"
#include "util/macro.h"

#include <stdlib.h>

void ir_scope_free(ir_scope *self) {
  if (self) {
    free(self);
  }
}

ir_scope *ir_scope_new(ir_scope_enum type) {
  ir_scope *self = MALLOC(ir_scope);
  self->type = type;
  return self;
}

static void container_delete_scope(void *data) {
  ir_scope *scope = data;
  ir_scope_free(scope);
}

scope_stack ir_scope_stack_new() {
  return (scope_stack){
      .list = list_new(container_cmp_false, container_new_move,
                       container_delete_scope),
  };
}

void ir_scope_stack_free(scope_stack stack) { return list_free(stack.list); }

void ir_scope_stack_push(scope_stack stack, ir_scope_enum type) {
  list_push_front(stack.list, ir_scope_new(type));
}

ir_scope_enum ir_scope_stack_pop(scope_stack stack) {
  ir_scope     *scope = list_pop_front(stack.list);
  ir_scope_enum type = scope->type;
  ir_scope_free(scope);
  return type;
}
