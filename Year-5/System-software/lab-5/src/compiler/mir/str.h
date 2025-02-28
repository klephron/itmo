#pragma once

#include "compiler/mir/mir.h"

#define MIR_STR_TAB 2
#define MIR_STR_SPACE " "

char *mir_lit_str(const mir_lit *self);

const char *mir_stmt_enum_str(mir_stmt_enum kind);
const char *mir_stmt_builtin_enum_str(mir_stmt_builtin_enum kind);
const char *mir_stmt_op_enum_str(mir_stmt_op_enum kind);

char *mir_str(const mir *mir);
