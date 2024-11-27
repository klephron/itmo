#pragma once

#include "compiler/eir/eir.h"
#include "dot.h"

dot_string *dot_eir_cfg(const eir *eir, const eir_subroutine *sub,
                        int f_add_expr);
dot_string *dot_eir_cg(const eir *eir, const eir_subroutine *sub);
