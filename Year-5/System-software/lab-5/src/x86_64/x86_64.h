#pragma once

#include <stdint.h>

typedef struct __attribute__((packed)) x86_64_value_struct x86_64_value;

typedef enum x86_64_type_enum {
  X86_64_TYPE_UNKNOWN,
  X86_64_TYPE_BOOL,
  X86_64_TYPE_BYTE,
  X86_64_TYPE_INT,
  X86_64_TYPE_UINT,
  X86_64_TYPE_LONG,
  X86_64_TYPE_ULONG,
  X86_64_TYPE_CHAR,
  X86_64_TYPE_STRING,
  X86_64_TYPE_ARRAY,
  X86_64_TYPE_OBJECT,
  X86_64_TYPE_REFERENCE,
} x86_64_type_enum;

typedef x86_64_value x86_64_op_plus(x86_64_value);
typedef x86_64_value x86_64_op_minus(x86_64_value);
typedef x86_64_value x86_64_op_not(x86_64_value);
typedef x86_64_value x86_64_op_bit_not(x86_64_value);
typedef x86_64_value x86_64_op_inc(x86_64_value);
typedef x86_64_value x86_64_op_dec(x86_64_value);
typedef x86_64_value x86_64_op_or(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_and(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_bit_or(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_bit_xor(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_bit_and(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_eq(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_neq(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_less(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_less_eq(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_bit_shl(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_bit_shr(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_add(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_sub(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_mul(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_div(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_rem(x86_64_value, x86_64_value);
typedef x86_64_value x86_64_op_call(x86_64_value, ...);
typedef x86_64_value x86_64_op_index(x86_64_value, ...);
typedef x86_64_value x86_64_op_repr(x86_64_value);
typedef void         x86_64_op_drop(x86_64_value *);

typedef struct __attribute__((packed)) x86_64_op_tbl_struct {
  x86_64_op_plus    *op_plus;
  x86_64_op_minus   *op_minus;
  x86_64_op_not     *op_not;
  x86_64_op_bit_not *op_bit_not;
  x86_64_op_inc     *op_inc;
  x86_64_op_dec     *op_dec;
  x86_64_op_or      *op_or;
  x86_64_op_and     *op_and;
  x86_64_op_bit_or  *op_bit_or;
  x86_64_op_bit_xor *op_bit_xor;
  x86_64_op_bit_and *op_bit_and;
  x86_64_op_eq      *op_eq;
  x86_64_op_neq     *op_neq;
  x86_64_op_less    *op_less;
  x86_64_op_less_eq *op_less_eq;
  x86_64_op_bit_shl *op_bit_shl;
  x86_64_op_bit_shr *op_bit_shr;
  x86_64_op_add     *op_add;
  x86_64_op_mul     *op_mul;
  x86_64_op_div     *op_div;
  x86_64_op_rem     *op_rem;
  x86_64_op_call    *op_call;
  x86_64_op_index   *op_index;
  x86_64_op_repr    *op_repr;
  x86_64_op_drop    *op_drop;
} x86_64_op_tbl;

// default implementations
x86_64_op_plus    __x86_64_op_base_plus;
x86_64_op_minus   __x86_64_op_base_minus;
x86_64_op_not     __x86_64_op_base_not;
x86_64_op_bit_not __x86_64_op_base_bit_not;
x86_64_op_inc     __x86_64_op_base_inc;
x86_64_op_dec     __x86_64_op_base_dec;
x86_64_op_or      __x86_64_op_base_or;
x86_64_op_and     __x86_64_op_base_and;
x86_64_op_bit_or  __x86_64_op_base_bit_or;
x86_64_op_bit_xor __x86_64_op_base_bit_xor;
x86_64_op_bit_and __x86_64_op_base_bit_and;
x86_64_op_eq      __x86_64_op_base_eq;
x86_64_op_neq     __x86_64_op_base_neq;
x86_64_op_less    __x86_64_op_base_less;
x86_64_op_less_eq __x86_64_op_base_less_eq;
x86_64_op_bit_shl __x86_64_op_base_bit_shl;
x86_64_op_bit_shr __x86_64_op_base_bit_shr;
x86_64_op_add     __x86_64_op_base_add;
x86_64_op_mul     __x86_64_op_base_mul;
x86_64_op_div     __x86_64_op_base_div;
x86_64_op_rem     __x86_64_op_base_rem;
x86_64_op_call    __x86_64_op_base_call;
x86_64_op_index   __x86_64_op_base_index;
x86_64_op_repr    __x86_64_op_base_repr;
x86_64_op_drop    __x86_64_op_base_drop;

typedef struct __attribute__((packed)) x86_64_data_struct {
  x86_64_type_enum type;
  uint64_t         ref_cnt;
  x86_64_op_tbl   *op_tbl;
} x86_64_data;

struct __attribute__((packed)) x86_64_value_struct {
  x86_64_data *data;
};

x86_64_value __x86_64_make_void();
