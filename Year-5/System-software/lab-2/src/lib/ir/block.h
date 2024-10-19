#pragma once

enum ir_block_enum { IR_BLOCK_UNKNOWN, IR_BLOCK_NEXT, IR_BLOCK_COND };

struct ir_block_base {
  enum ir_block_enum kind;
};

struct ir_block_next {
  struct ir_block_base  base;
  struct ir_block_base *next;
  void                 *expr;
};

struct ir_block_cond {
  struct ir_block_base  base;
  struct ir_block_base *jz;
  struct ir_block_base *je;
  void                 *expr;
};

typedef enum ir_block_enum   ir_block_enum;
typedef struct ir_block_base ir_block_base;
typedef struct ir_block_next ir_block_next;
typedef struct ir_block_cond ir_block_cond;

ir_block_next *ir_block_next_new();
ir_block_cond *ir_block_cond_new();
