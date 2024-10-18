#pragma once

#include "ir/ctx.h"
#include "ir/functions.h"
#include "ir/pass.h"

struct ir_pass1 {
  struct ir_pass_base base;
  struct ir_ctx      *ctx;
  const char         *file_name;
};

typedef struct ir_pass1 ir_pass1;

void ir_pass1_init(ir_pass1 *self, struct ir_ctx *ctx, const char *file_name);

ir_subroutine *ir_pass1_subroutine_new(const ir_pass1 *self,
                                       ir_signature   *signature);
int ir_pass1_subroutine_add(ir_pass1 *self, ir_subroutine *subroutine);

int ir_pass1_param_add(ir_pass1 *self, p_list list, ir_param *param);

#define IR_PASS1_ANTLR3_USERP()                                                \
  ((ir_pass1 *)ctx->pTreeParser->rec->state->userp)
