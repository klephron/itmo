#pragma once

#include "util/list.h"

struct option2_inter_userp {
  struct option2_inter *inter;
  const char *file_name;
};

struct option2_inter {
  p_list subroutines; // option2_inter_subroutine
  p_list exceptions;  // exception

  void (*free)(struct option2_inter *);
};

typedef struct option2_inter_userp option2_inter_userp;
typedef struct option2_inter option2_inter;

struct option2_inter *option2_inter_build(const p_list asts);
