#pragma once

#include "x86_64/value.h"
#include <stdint.h>

typedef struct __attribute__((packed)) x86_64_data_long_struct {
  int64_t value;
} x86_64_data_long;

void __x86_64_long_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                        int64_t value);
