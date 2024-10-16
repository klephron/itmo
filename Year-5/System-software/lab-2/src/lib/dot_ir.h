#pragma once

#include "dot.h"
#include "ir/ctx.h"
#include "ir/functions.h"

dot_string *dot_inter_cfg(const ir_ctx *ctx, const ir_subroutine *sub);
dot_string *dot_inter_cg(const ir_ctx *ctx, const ir_subroutine *sub);
