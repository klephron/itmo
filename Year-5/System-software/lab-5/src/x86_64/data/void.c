#include "void.h"

#include "util/macro.h"
#include "x86_64/builtin/builtin.h"
#include "x86_64/value.h"
#include <stdlib.h>

x86_64_data_void *__x86_64_void_new(x86_64_op_tbl *op_tbl) {
  x86_64_data_void *self = MALLOC(x86_64_data_void);
  self->base.type = X86_64_TYPE_VOID;
  self->base.ref_cnt = 0;
  self->base.op_tbl = op_tbl;
  return self;
}

x86_64_value __x86_64_void_op_plus(x86_64_value first) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_minus(x86_64_value first) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_not(x86_64_value first) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_bit_not(x86_64_value first) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_inc(x86_64_value first) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_dec(x86_64_value first) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_or(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_and(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_bit_or(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_bit_xor(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_bit_and(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_eq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_neq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_less(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_less_eq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_bit_shl(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_bit_shr(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_add(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_mul(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_div(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_rem(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_call(x86_64_value first, ...) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_index(x86_64_value first, ...) {
  UNUSED(first);
  return __x86_64_make_void();
}

x86_64_value __x86_64_void_op_repr(x86_64_value first) {
  UNUSED(first);
  return __x86_64_make_void();
}

void __x86_64_void_op_drop(x86_64_value *first) {
  if (--first->data->ref_cnt == 0) {
    free(first->data);
    *first = __x86_64_make_void();
  }
}
