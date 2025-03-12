#pragma once

#include "x86_64/data.h"

typedef struct __attribute__((packed)) x86_64_data_ulong_struct {
  x86_64_data base;
  uint64_t    value;
} x86_64_data_ulong;

x86_64_data_ulong *__x86_64_ulong_new(x86_64_op_tbl *op_tbl, uint64_t value);

x86_64_op_plus    __x86_64_ulong_op_plus;
x86_64_op_minus   __x86_64_ulong_op_minus;
x86_64_op_not     __x86_64_ulong_op_not;
x86_64_op_bit_not __x86_64_ulong_op_bit_not;
x86_64_op_inc     __x86_64_ulong_op_inc;
x86_64_op_dec     __x86_64_ulong_op_dec;
x86_64_op_or      __x86_64_ulong_op_or;
x86_64_op_and     __x86_64_ulong_op_and;
x86_64_op_bit_or  __x86_64_ulong_op_bit_or;
x86_64_op_bit_xor __x86_64_ulong_op_bit_xor;
x86_64_op_bit_and __x86_64_ulong_op_bit_and;
x86_64_op_eq      __x86_64_ulong_op_eq;
x86_64_op_neq     __x86_64_ulong_op_neq;
x86_64_op_less    __x86_64_ulong_op_less;
x86_64_op_less_eq __x86_64_ulong_op_less_eq;
x86_64_op_bit_shl __x86_64_ulong_op_bit_shl;
x86_64_op_bit_shr __x86_64_ulong_op_bit_shr;
x86_64_op_add     __x86_64_ulong_op_add;
x86_64_op_mul     __x86_64_ulong_op_mul;
x86_64_op_div     __x86_64_ulong_op_div;
x86_64_op_rem     __x86_64_ulong_op_rem;
x86_64_op_call    __x86_64_ulong_op_call;
x86_64_op_index   __x86_64_ulong_op_index;
x86_64_op_repr    __x86_64_ulong_op_repr;
x86_64_op_drop    __x86_64_ulong_op_drop;
