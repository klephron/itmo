#pragma once

#include "option2_ir/ctx.h"

struct option2_ir_userp {
  struct option2_ir_ctx *ctx;
  const char *file_name;
};

typedef struct option2_ir_userp option2_ir_userp;
