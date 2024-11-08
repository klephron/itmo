#pragma once

#include "exception.h"
#include "util/hashset.h"
#include "util/list.h"

// created before passes/stages
struct ir_ctx {
  p_hashset subroutines; // ir_subroutine
  p_list    exceptions;  // exception
};

typedef struct ir_ctx ir_ctx;

ir_ctx *ir_ctx_new();
void    ir_ctx_free(struct ir_ctx *);

void ir_ctx_add_exception(ir_ctx *self, exception *ex);
