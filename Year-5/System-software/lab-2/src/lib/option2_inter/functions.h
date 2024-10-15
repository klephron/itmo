#pragma once

#include "option2_inter/types.h"
#include "option2_inter/userp.h"
#include "util/list.h"

struct option2_inter_signature {
  char *name;
  p_list params;
  struct option2_inter_t_base *ret;
};

struct option2_inter_subroutine {
  struct option2_inter_signature *signature;
  char *file_name;
  void *blocks;
  p_list operations; // tree-like structure
};

typedef struct option2_inter_subroutine option2_inter_subroutine;
typedef struct option2_inter_signature option2_inter_signature;

void option2_inter_subroutine_free(option2_inter_subroutine *data);

option2_inter_signature *option2_inter_signature_new(char *name, p_list params,
                                                     option2_inter_t_base *ret);
option2_inter_subroutine *
option2_inter_subroutine_new(const option2_inter_userp *userp,
                             option2_inter_signature *signature, void *blocks,
                             p_list operations);
