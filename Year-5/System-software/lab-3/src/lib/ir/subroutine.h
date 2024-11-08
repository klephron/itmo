#pragma once

#include "ir/block.h"
#include "ir/terminal.h"
#include "util/hashset.h"
#include "util/list.h"

struct ir_var {
  ir_t_base  *type;
  ir_term_id *id;
};

struct ir_signature {
  ir_term_id *id;
  p_list      params; // ir_var
  ir_t_base  *ret;
};

struct ir_subroutine {
  struct ir_signature *signature;
  char                *file_name;
  ir_block_base       *cfg;
  p_list               vars;
};

enum ir_subroutine_status_enum {
  IR_SUBROUTINE_STATUS_ABSENT,
  IR_SUBROUTINE_STATUS_DECLARATION,
  IR_SUBROUTINE_STATUS_DEFINITION
};

enum ir_subroutine_cmp_enum {
  IR_SUBROUTINE_CMP_SAME = 0,
  IR_SUBROUTINE_CMP_NULL = 0b1,
  IR_SUBROUTINE_CMP_RET = 0b10,
  IR_SUBROUTINE_CMP_PARAMS = 0b100,
  IR_SUBROUTINE_CMP_NAME = 0b1000,
};

typedef struct ir_var                  ir_var;
typedef struct ir_subroutine           ir_subroutine;
typedef struct ir_signature            ir_signature;
typedef enum ir_subroutine_status_enum ir_subroutine_status_enum;
typedef enum ir_subroutine_cmp_enum    ir_subroutine_cmp_enum;

ir_var     *ir_var_new(ir_term_id *id, ir_t_base *type);
const char *ir_var_name(const ir_var *self);
int         ir_var_cmp(const ir_var *lsv, const ir_var *rsv);

p_list    ir_var_list_new();
p_hashset ir_var_hashset_new();

ir_signature *ir_signature_new(ir_term_id *id, p_list params, ir_t_base *ret);

void           ir_subroutine_free(ir_subroutine *self);
ir_subroutine *ir_subroutine_new(const char *file_name, ir_signature *signature,
                                 void *blocks, p_list vars);
ir_subroutine_status_enum ir_subroutine_status(const ir_subroutine *self);
const char               *ir_subroutine_name(const ir_subroutine *self);
int ir_subroutine_cmp(const ir_subroutine *lsv, const ir_subroutine *rsv);
