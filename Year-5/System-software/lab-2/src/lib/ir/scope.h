#pragma once

#include "ir/block.h"
#include "ir/subroutine.h"
#include "util/hashset.h"
#include "util/list.h"

enum ir_scope_enum {
  IR_SCOPE_UNKNOWN,
  IR_SCOPE_GLOBAL,
  IR_SCOPE_SUBROUTINE,
  IR_SCOPE_BLOCK,
  IR_SCOPE_INLINE
};

enum ir_scope_symbol_enum {
  IR_SCOPE_SYMBOL_UNKNOWN,
  IR_SCOPE_SYMBOL_SUBROUTINE,
  IR_SCOPE_SYMBOL_VARIABLE
};

struct ir_scope_symbol_base {
  enum ir_scope_symbol_enum kind;
};

struct ir_scope_symbol_sub {
  struct ir_scope_symbol_base base;
  ir_subroutine              *value;
};

struct ir_scope_symbol_var {
  struct ir_scope_symbol_base base;
  ir_var                     *value;
};

struct ir_scope_symbol_kv {
  ir_term_id                  *k;
  struct ir_scope_symbol_base *v;
};

struct ir_scope {
  enum ir_scope_enum type;
  ir_block_base     *begin;
  ir_block_base     *end;
  ir_block_base     *cur;

  p_hashset symbols;
};

struct scope_stack {
  p_list list;
};

typedef enum ir_scope_enum          ir_scope_enum;
typedef enum ir_scope_symbol_enum   ir_scope_symbol_enum;
typedef struct ir_scope_symbol_base ir_scope_symbol_base;
typedef struct ir_scope_symbol_sub  ir_scope_symbol_sub;
typedef struct ir_scope_symbol_var  ir_scope_symbol_var;
typedef struct ir_scope_symbol_kv   ir_scope_symbol_kv;
typedef struct ir_scope             ir_scope;
typedef struct ir_scope_data        ir_scope_data;
typedef struct scope_stack          scope_stack;

ir_scope *ir_scope_new(ir_scope_enum type, ir_block_base *begin,
                       ir_block_base *end, ir_block_base *cur);
void      ir_scope_free(ir_scope *);

void ir_scope_symbol_free(ir_scope_symbol_base *generic);

ir_scope_symbol_sub *ir_scope_symbol_sub_new(ir_subroutine *sub);
ir_scope_symbol_var *ir_scope_symbol_var_new(ir_var *var);
int                  ir_scope_symbol_cmp(const struct ir_scope_symbol_base *lsv,
                                         const struct ir_scope_symbol_base *rsv);

ir_scope_symbol_kv *ir_scope_symbol_kv_new(ir_term_id           *k,
                                           ir_scope_symbol_base *v);
void                ir_scope_symbol_kv_free(ir_scope_symbol_kv *kv);

ir_term_id *ir_scope_symbol_k_sub(ir_subroutine *sub);
ir_term_id *ir_scope_symbol_k_var(ir_var *var);

void ir_scope_symbol_add(ir_scope *scope, ir_scope_symbol_kv *kv);
void ir_scope_symbol_add_sub(ir_scope *scope, ir_subroutine *sub);
void ir_scope_symbol_add_var(ir_scope *scope, ir_var *var);
ir_scope_symbol_base *ir_scope_symbol_find(ir_scope *scope, ir_term_id *id);

scope_stack ir_scope_stack_new();
void        ir_scope_stack_free(scope_stack stack);
void        ir_scope_stack_push(scope_stack stack, ir_scope *scope);
ir_scope   *ir_scope_stack_pop(scope_stack stack);
ir_scope   *ir_scope_stack_top(scope_stack stack);

ir_scope_symbol_base *ir_scope_stack_find_symbol(scope_stack stack,
                                                 ir_term_id *id);
