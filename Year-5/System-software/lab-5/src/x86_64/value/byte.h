#pragma once

#include "x86_64/value.h"
#include <stdint.h>

typedef struct __attribute__((packed)) x86_64_data_byte_struct {
  uint8_t value;
  uint8_t pad[7];
} x86_64_data_byte;

void __x86_64_byte_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                        uint8_t value);
