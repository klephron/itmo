#pragma once

#include "dot.h"
#include "option2_ir/ctx.h"
#include "option2_ir/functions.h"

dot_string *dot_inter_cfg(const option2_ir_ctx *ctx,
                          const option2_ir_subroutine *sub);
dot_string *dot_inter_cg(const option2_ir_ctx *ctx,
                         const option2_ir_subroutine *sub);
