#pragma once

#include "compiler/codegen/x86_64/x86_64.h"
#include "compiler/exception/list.h"
#include "compiler/mir/mir.h"

typedef struct cg_inst_dir_result_struct {
  list_exception *exceptions;
} cg_inst_dir_result;

cg_inst_dir_result cg_inst_dir(cg_x86_64 *code, const mir *mir);
