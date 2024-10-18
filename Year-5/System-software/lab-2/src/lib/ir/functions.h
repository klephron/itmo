#pragma once

#include "ir/terminals.h"
#include "ir/types.h"
#include "util/list.h"

struct ir_param {
  ir_t_base  *type;
  ir_term_id *id;
};

struct ir_signature {
  ir_term_id *id;
  p_list      params; // ir_param
  ir_t_base  *ret;
};

struct ir_subroutine {
  struct ir_signature *signature;
  char                *file_name;
  void                *blocks;
  p_list               operations; // tree-like structure
};

enum ir_subroutine_status_enum {
  IR_SUBROUTINE_STATUS_ABSENT = 0,
  IR_SUBROUTINE_STATUS_DECLARATION = 1,
  IR_SUBROUTINE_STATUS_DEFINITION = 2
};

enum ir_subroutine_cmp_enum {
  IR_SUBROUTINE_CMP_SAME = 0,
  IR_SUBROUTINE_CMP_NULL = 0b1,
  IR_SUBROUTINE_CMP_RET = 0b10,
  IR_SUBROUTINE_CMP_PARAMS = 0b100,
  IR_SUBROUTINE_CMP_ID = 0b1000,
};

typedef struct ir_param                ir_param;
typedef struct ir_subroutine           ir_subroutine;
typedef struct ir_signature            ir_signature;
typedef enum ir_subroutine_status_enum ir_subroutine_status_enum;
typedef enum ir_subroutine_cmp_enum    ir_subroutine_cmp_enum;

ir_param   *ir_param_new(ir_term_id *id, ir_t_base *type);
p_list      ir_param_list_new();
const char *ir_param_name(const ir_param *self);

ir_signature *ir_signature_new(ir_term_id *id, p_list params, ir_t_base *ret);

void           ir_subroutine_free(ir_subroutine *self);
ir_subroutine *ir_subroutine_new(const char *file_name, ir_signature *signature,
                                 void *blocks, p_list operations);
ir_subroutine_status_enum ir_subroutine_status(const ir_subroutine *self);
const char               *ir_subroutine_name(const ir_subroutine *self);
int ir_subroutine_cmp(const ir_subroutine *lsv, const ir_subroutine *rsv);
