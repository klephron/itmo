#pragma once

#include <stdint.h>
typedef struct __attribute__((packed)) x86_64_value_struct x86_64_value;

typedef enum x86_64_type_enum {
  X86_64_TYPE_VOID,
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
  X86_64_TYPE_REF,
  X86_64_TYPE_ERROR,
  X86_64_TYPE__COUNT,
} x86_64_type_enum;

typedef void x86_64_op_plus(x86_64_value *out, x86_64_value *self);
typedef void x86_64_op_minus(x86_64_value *out, x86_64_value *self);
typedef void x86_64_op_not(x86_64_value *out, x86_64_value *self);
typedef void x86_64_op_bit_not(x86_64_value *out, x86_64_value *self);
typedef void x86_64_op_inc(x86_64_value *out, x86_64_value *self);
typedef void x86_64_op_dec(x86_64_value *out, x86_64_value *self);
typedef void x86_64_op_or(x86_64_value *out, x86_64_value *self,
                          x86_64_value *rsv);
typedef void x86_64_op_and(x86_64_value *out, x86_64_value *self,
                           x86_64_value *rsv);
typedef void x86_64_op_bit_or(x86_64_value *out, x86_64_value *self,
                              x86_64_value *rsv);
typedef void x86_64_op_bit_xor(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv);
typedef void x86_64_op_bit_and(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv);
typedef void x86_64_op_eq(x86_64_value *out, x86_64_value *self,
                          x86_64_value *rsv);
typedef void x86_64_op_neq(x86_64_value *out, x86_64_value *self,
                           x86_64_value *rsv);
typedef void x86_64_op_less(x86_64_value *out, x86_64_value *self,
                            x86_64_value *rsv);
typedef void x86_64_op_less_eq(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv);
typedef void x86_64_op_bit_shl(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv);
typedef void x86_64_op_bit_shr(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv);
typedef void x86_64_op_add(x86_64_value *out, x86_64_value *self,
                           x86_64_value *rsv);
typedef void x86_64_op_sub(x86_64_value *out, x86_64_value *self,
                           x86_64_value *rsv);
typedef void x86_64_op_mul(x86_64_value *out, x86_64_value *self,
                           x86_64_value *rsv);
typedef void x86_64_op_div(x86_64_value *out, x86_64_value *self,
                           x86_64_value *rsv);
typedef void x86_64_op_rem(x86_64_value *out, x86_64_value *self,
                           x86_64_value *rsv);
typedef void x86_64_op_call(x86_64_value *out, x86_64_value *self, ...);
typedef void x86_64_op_index(x86_64_value *out, x86_64_value *self, ...);
// should return value of type string, accessed by out->data_raw
typedef void x86_64_op_repr(x86_64_value *out, x86_64_value *self);
typedef void x86_64_op_drop(x86_64_value *out);
typedef void x86_64_op_copy(x86_64_value *out, x86_64_value *self);
// logic is defined by value to which is assignment done (other is copied)
typedef void x86_64_op_assign(x86_64_value *self, x86_64_value *other);
typedef void x86_64_op_assign_deref(x86_64_value *self, x86_64_value *other);

typedef struct __attribute__((packed)) x86_64_op_tbl_struct {
  x86_64_op_plus         *op_plus;
  x86_64_op_minus        *op_minus;
  x86_64_op_not          *op_not;
  x86_64_op_bit_not      *op_bit_not;
  x86_64_op_inc          *op_inc;
  x86_64_op_dec          *op_dec;
  x86_64_op_or           *op_or;
  x86_64_op_and          *op_and;
  x86_64_op_bit_or       *op_bit_or;
  x86_64_op_bit_xor      *op_bit_xor;
  x86_64_op_bit_and      *op_bit_and;
  x86_64_op_eq           *op_eq;
  x86_64_op_neq          *op_neq;
  x86_64_op_less         *op_less;
  x86_64_op_less_eq      *op_less_eq;
  x86_64_op_bit_shl      *op_bit_shl;
  x86_64_op_bit_shr      *op_bit_shr;
  x86_64_op_add          *op_add;
  x86_64_op_mul          *op_mul;
  x86_64_op_div          *op_div;
  x86_64_op_rem          *op_rem;
  x86_64_op_call         *op_call;
  x86_64_op_index        *op_index;
  x86_64_op_repr         *op_repr;
  x86_64_op_drop         *op_drop;
  x86_64_op_copy         *op_copy;
  x86_64_op_assign       *op_assign;
  x86_64_op_assign_deref *op_assign_deref;
} x86_64_op_tbl;

typedef struct __attribute__((packed)) x86_64_value_struct {
  struct {
    x86_64_type_enum type;
    uint8_t          type_pad[4];
  };
  x86_64_op_tbl *op_tbl;
  union {
    void    *data_ptr;
    uint64_t data_raw;
  };
} x86_64_value;

static inline void __x86_64_value_init_ptr(x86_64_value    *value,
                                           x86_64_type_enum type,
                                           x86_64_op_tbl *op_tbl, void *data) {
  value->type = type;
  value->op_tbl = op_tbl;
  value->data_ptr = data;
}

static inline void __x86_64_value_init_raw(x86_64_value    *value,
                                           x86_64_type_enum type,
                                           x86_64_op_tbl   *op_tbl,
                                           uint64_t         data) {
  value->type = type;
  value->op_tbl = op_tbl;
  value->data_raw = data;
}
