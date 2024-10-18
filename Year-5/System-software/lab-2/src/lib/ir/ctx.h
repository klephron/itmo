#pragma once

#include "exception.h"
#include "util/list.h"

// created before passes/stages
struct ir_ctx {
  p_list subroutines; // inter_subroutine
  p_list exceptions;  // exception

  void (*free)(struct ir_ctx *);
};

typedef struct ir_ctx ir_ctx;

ir_ctx *ir_ctx_new();

void ir_ctx_add_exception(ir_ctx *self, exception *ex);
