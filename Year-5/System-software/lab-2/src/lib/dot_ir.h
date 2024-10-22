#pragma once

#include "dot.h"
#include "ir/ctx.h"
#include "ir/function.h"

dot_string *dot_ir_cfg(const ir_ctx *ctx, const ir_subroutine *sub, int v_expr);
dot_string *dot_ir_cg(const ir_ctx *ctx, const ir_subroutine *sub);
