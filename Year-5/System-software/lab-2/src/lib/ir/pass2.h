#pragma once

#include "ir/pass.h"

struct ir_pass2 {
  struct ir_pass_base base;
  struct ir_ctx *ctx;
  const char *file_name;
};

typedef struct ir_pass2 ir_pass2;

void ir_pass2_init(ir_pass2 *self, struct ir_ctx *ctx, const char *file_name);

#define IR_PASS2_ANTLR3_USERP()                                                \
  ((ir_pass2 *)ctx->pTreeParser->rec->state->userp)
