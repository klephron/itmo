#pragma once

#include "exception.h"
#include "ir/functions.h"
#include "util/list.h"

struct ir_ctx {
  p_list subroutines; // inter_subroutine
  p_list exceptions;  // exception

  void (*free)(struct ir_ctx *);
};

typedef struct ir_ctx ir_ctx;

ir_ctx *ir_ctx_new();

int ctx_add_subroutine(ir_ctx *ctx, struct ir_subroutine *subroutine);
void ctx_add_exception(ir_ctx *ctx, exception *ex);
