#pragma once

#include "x86_64/value.h"

typedef struct __attribute__((packed)) x86_64_data_array_struct {
  uint64_t     ref_cnt;
  uint64_t     length;
  x86_64_value elements[0];
} x86_64_data_array;

void __x86_64_array_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                         uint64_t length);
