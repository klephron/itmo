#pragma once

#include "ir/expr.h"

enum ir_block_enum { IR_BLOCK_UNKNOWN, IR_BLOCK_NEXT, IR_BLOCK_COND };

enum ir_block_status_enum { IR_BLOCK_STATUS_OK, IR_BLOCK_STATUS_UNTIED };

struct ir_block_base {
  enum ir_block_enum        kind;
  enum ir_block_status_enum status;
};

struct ir_block_next {
  struct ir_block_base  base;
  struct ir_block_base *next;
  ir_expr_base         *expr;
};

struct ir_block_cond {
  struct ir_block_base  base;
  struct ir_block_base *jz;
  struct ir_block_base *je;
  ir_expr_base         *cond;
};

typedef enum ir_block_enum        ir_block_enum;
typedef enum ir_block_status_enum ir_block_status_enum;
typedef struct ir_block_base      ir_block_base;
typedef struct ir_block_next      ir_block_next;
typedef struct ir_block_cond      ir_block_cond;

void ir_block_free(ir_block_base *generic);

ir_block_next *ir_block_next_new(ir_block_base *next, ir_expr_base *expr);
ir_block_cond *ir_block_cond_new(ir_block_base *jz, ir_block_base *je,
                                 ir_expr_base *cond);

void ir_block_graph_free(ir_block_base *root);

void           ir_block_set_next(ir_block_base *self, ir_block_base *next);
ir_block_base *ir_block_get_next(ir_block_base *self);
