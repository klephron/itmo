#pragma once

#include "x86_64/data.h"

typedef struct __attribute__((packed)) x86_64_data_void_struct {
  x86_64_data base;
} x86_64_data_void;

// allocates it as everything else
x86_64_data_void *__x86_64_new_void(x86_64_op_tbl *op_tbl);

x86_64_op_plus    __x86_64_op_void_plus;
x86_64_op_minus   __x86_64_op_void_minus;
x86_64_op_not     __x86_64_op_void_not;
x86_64_op_bit_not __x86_64_op_void_bit_not;
x86_64_op_inc     __x86_64_op_void_inc;
x86_64_op_dec     __x86_64_op_void_dec;
x86_64_op_or      __x86_64_op_void_or;
x86_64_op_and     __x86_64_op_void_and;
x86_64_op_bit_or  __x86_64_op_void_bit_or;
x86_64_op_bit_xor __x86_64_op_void_bit_xor;
x86_64_op_bit_and __x86_64_op_void_bit_and;
x86_64_op_eq      __x86_64_op_void_eq;
x86_64_op_neq     __x86_64_op_void_neq;
x86_64_op_less    __x86_64_op_void_less;
x86_64_op_less_eq __x86_64_op_void_less_eq;
x86_64_op_bit_shl __x86_64_op_void_bit_shl;
x86_64_op_bit_shr __x86_64_op_void_bit_shr;
x86_64_op_add     __x86_64_op_void_add;
x86_64_op_mul     __x86_64_op_void_mul;
x86_64_op_div     __x86_64_op_void_div;
x86_64_op_rem     __x86_64_op_void_rem;
x86_64_op_call    __x86_64_op_void_call;
x86_64_op_index   __x86_64_op_void_index;
x86_64_op_repr    __x86_64_op_void_repr;
x86_64_op_drop    __x86_64_op_void_drop;
