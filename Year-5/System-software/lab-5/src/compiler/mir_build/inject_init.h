#pragma once

#include "compiler/exception/list.h"
#include "compiler/mir/mir.h"

typedef struct mir_inject_init_result_struct {
  symbol_table   *symbol_table;
  list_exception *exceptions;
} mir_inject_init_result;

mir_inject_init_result mir_inject_init(mir *mir);
