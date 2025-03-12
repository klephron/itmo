#pragma once

#include "x86_64/data.h"
#include "x86_64/data/void.h"

typedef struct x86_64_registry_struct {
  const x86_64_op_tbl **op_tbl_arr;
  // used to initialize variables, but not allocate memory
  const x86_64_data_void data_void;
} x86_64_registry;

extern const x86_64_registry X86_64_REGISTRY;
