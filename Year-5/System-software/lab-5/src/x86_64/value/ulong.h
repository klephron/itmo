#pragma once

#include "x86_64/value.h"
#include <stdint.h>

typedef struct __attribute__((packed)) x86_64_data_ulong_struct {
  uint64_t value;
} x86_64_data_ulong;

void __x86_64_ulong_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                         uint64_t value);
