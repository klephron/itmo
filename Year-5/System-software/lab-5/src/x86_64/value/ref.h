#pragma once

#include "x86_64/value.h"

void __x86_64_ref_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                       x86_64_value *value);
