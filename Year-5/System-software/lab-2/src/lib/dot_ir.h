#pragma once

#include "dot.h"
#include "ir/ctx.h"
#include "ir/functions.h"

dot_string *dot_ir_cfg(const ir_ctx *ctx, const ir_subroutine *sub);
dot_string *dot_ir_cg(const ir_ctx *ctx, const ir_subroutine *sub);
