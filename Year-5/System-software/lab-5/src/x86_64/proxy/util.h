#pragma once

#include "x86_64/value.h"

void __x86_64_proxy_op_error_undefined(x86_64_value *out, x86_64_value *self,
                                       const char *op);

void __x86_64_proxy_op_error_type_mismatch(x86_64_value *out,
                                           x86_64_value *self, const char *op,
                                           x86_64_type_enum type);

void __x86_64_proxy_op_error_cast_unable(x86_64_value *out, x86_64_value *self,
                                         x86_64_type_enum type);
