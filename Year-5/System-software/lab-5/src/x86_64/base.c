#include "base.h"

#include "builtin/builtin.h"
#include "value.h"

#include "util/log.h"
#include "util/macro.h"

#include <stddef.h>

static inline void x86_64_base_op_error(const char *op) {
  error("basic implementation of operation %s is not defined", op);
}

x86_64_value __x86_64_base_op_plus(x86_64_value first) {
  UNUSED(first);
  x86_64_base_op_error("plus");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_minus(x86_64_value first) {
  UNUSED(first);
  x86_64_base_op_error("minus");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_not(x86_64_value first) {
  UNUSED(first);
  x86_64_base_op_error("not");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_bit_not(x86_64_value first) {
  UNUSED(first);
  x86_64_base_op_error("bit_not");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_inc(x86_64_value first) {
  UNUSED(first);
  x86_64_base_op_error("inc");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_dec(x86_64_value first) {
  UNUSED(first);
  x86_64_base_op_error("dec");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_or(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("or");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_and(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("and");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_bit_or(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("bit_or");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_bit_xor(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("bit_xor");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_bit_and(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("bit_and");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_eq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("eq");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_neq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("neq");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_less(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("less");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_less_eq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("less_eq");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_bit_shl(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("bit_shl");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_bit_shr(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("bit_shr");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_add(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("add");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_mul(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("mul");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_div(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("div");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_rem(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  x86_64_base_op_error("rem");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_call(x86_64_value first, ...) {
  UNUSED(first);
  x86_64_base_op_error("call");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_index(x86_64_value first, ...) {
  UNUSED(first);
  x86_64_base_op_error("index");
  return __x86_64_make_void();
}

x86_64_value __x86_64_base_op_repr(x86_64_value first) {
  UNUSED(first);
  x86_64_base_op_error("repr");
  return __x86_64_make_void();
}

void __x86_64_base_op_drop(x86_64_value *first) {
  UNUSED(first);
  x86_64_base_op_error("drop");
}
