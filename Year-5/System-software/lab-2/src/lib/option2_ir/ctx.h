#pragma once

#include "util/list.h"

struct option2_ir_ctx {
  p_list subroutines; // option2_inter_subroutine
  p_list exceptions;  // exception

  void (*free)(struct option2_ir_ctx *);
};

typedef struct option2_ir_ctx option2_ir_ctx;

option2_ir_ctx *option2_ir_ctx_new();
