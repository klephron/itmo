#pragma once

#include "dot.h"
#include "option2_inter/ctx.h"
#include "option2_inter/functions.h"

dot_string *dot_inter_cfg(const option2_inter *inter,
                          const option2_inter_subroutine *sub);
dot_string *dot_inter_cg(const option2_inter *inter,
                         const option2_inter_subroutine *sub);
