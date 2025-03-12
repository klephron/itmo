#pragma once

#include "x86_64/data.h"
#include "x86_64/value.h"

typedef struct __attribute__((packed)) x86_64_data_error_struct {
  x86_64_data  base;
  x86_64_value value;
} x86_64_data_error;

x86_64_data_error *__x86_64_error_new(x86_64_op_tbl *op_tbl,
                                      x86_64_value   value);

x86_64_op_plus    __x86_64_op_error_plus;
x86_64_op_minus   __x86_64_op_error_minus;
x86_64_op_not     __x86_64_op_error_not;
x86_64_op_bit_not __x86_64_op_error_bit_not;
x86_64_op_inc     __x86_64_op_error_inc;
x86_64_op_dec     __x86_64_op_error_dec;
x86_64_op_or      __x86_64_op_error_or;
x86_64_op_and     __x86_64_op_error_and;
x86_64_op_bit_or  __x86_64_op_error_bit_or;
x86_64_op_bit_xor __x86_64_op_error_bit_xor;
x86_64_op_bit_and __x86_64_op_error_bit_and;
x86_64_op_eq      __x86_64_op_error_eq;
x86_64_op_neq     __x86_64_op_error_neq;
x86_64_op_less    __x86_64_op_error_less;
x86_64_op_less_eq __x86_64_op_error_less_eq;
x86_64_op_bit_shl __x86_64_op_error_bit_shl;
x86_64_op_bit_shr __x86_64_op_error_bit_shr;
x86_64_op_add     __x86_64_op_error_add;
x86_64_op_mul     __x86_64_op_error_mul;
x86_64_op_div     __x86_64_op_error_div;
x86_64_op_rem     __x86_64_op_error_rem;
x86_64_op_call    __x86_64_op_error_call;
x86_64_op_index   __x86_64_op_error_index;
x86_64_op_repr    __x86_64_op_error_repr;
x86_64_op_drop    __x86_64_op_error_drop;
