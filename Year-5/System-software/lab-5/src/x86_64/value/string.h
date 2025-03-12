#pragma once

#include "x86_64/value.h"
#include <stdint.h>

void __x86_64_string_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                          const uint8_t *value);
