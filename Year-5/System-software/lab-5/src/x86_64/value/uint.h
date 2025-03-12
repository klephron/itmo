#pragma once

#include "x86_64/value.h"
#include <stdint.h>

typedef struct __attribute__((packed)) x86_64_data_uint_struct {
  uint32_t value;
  uint8_t  pad[4];
} x86_64_data_uint;

void __x86_64_uint_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                        uint32_t value);
