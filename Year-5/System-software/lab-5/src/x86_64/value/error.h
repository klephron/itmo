#pragma once

#include "x86_64/value.h"

typedef struct __attribute__((packed)) x86_64_data_error_struct {
  uint64_t     ref_cnt;
  x86_64_value value;
} x86_64_data_error;

void __x86_64_error_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                         x86_64_value *value);
