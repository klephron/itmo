#pragma once

#include "x86_64/value.h"
#include <stdint.h>

typedef struct __attribute__((packed)) x86_64_data_int_struct {
  int32_t value;
  uint8_t pad[4];
} x86_64_data_int;

void __x86_64_int_init(x86_64_value *out, x86_64_op_tbl *op_tbl, int32_t value);
