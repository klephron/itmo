#pragma once

#include "option2_ir/types.h"
#include "option2_ir/userp.h"
#include "util/list.h"

struct option2_ir_signature {
  char *name;
  p_list params;
  struct option2_ir_t_base *ret;
};

struct option2_ir_subroutine {
  struct option2_ir_signature *signature;
  char *file_name;
  void *blocks;
  p_list operations; // tree-like structure
};

typedef struct option2_ir_subroutine option2_ir_subroutine;
typedef struct option2_ir_signature option2_ir_signature;

void option2_ir_subroutine_free(option2_ir_subroutine *data);

option2_ir_signature *option2_ir_signature_new(char *name, p_list params,
                                                     option2_ir_t_base *ret);
option2_ir_subroutine *
option2_ir_subroutine_new(const option2_ir_userp *userp,
                             option2_ir_signature *signature, void *blocks,
                             p_list operations);
