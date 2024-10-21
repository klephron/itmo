#pragma once

#include "util/list.h"

enum ir_scope_enum { IR_SCOPE_UNKNOWN, IR_SCOPE_FUNCTION, IR_SCOPE_BLOCK };

struct ir_scope {
  enum ir_scope_enum type;
};

struct scope_stack {
  p_list list;
};

typedef enum ir_scope_enum ir_scope_enum;
typedef struct ir_scope    ir_scope;
typedef struct scope_stack scope_stack;

ir_scope   *ir_scope_new(ir_scope_enum type);
void        ir_scope_free(ir_scope *);
scope_stack ir_scope_stack_new();
void        ir_scope_stack_free(scope_stack stack);

void          ir_scope_stack_push(scope_stack stack, ir_scope_enum type);
ir_scope_enum ir_scope_stack_pop(scope_stack stack);
