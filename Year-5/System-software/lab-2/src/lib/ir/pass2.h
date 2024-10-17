#pragma once

struct ir_pass2 {
  struct ir_ctx *ctx;
  const char *file_name;
};

typedef struct ir_pass2 ir_pass2;

#define IR_PASS2_ANTLR3_USERP()                                                \
  ((ir_pass2 *)ctx->pTreeParser->rec->state->userp)
