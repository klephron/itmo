#pragma once

#include "ir/ctx.h"

struct ir_userp {
  struct ir_ctx *ctx;
  const char *file_name;
};

#define ANTLR3_USERP() ((ir_userp *)ctx->pTreeParser->rec->state->userp)

typedef struct ir_userp ir_userp;
