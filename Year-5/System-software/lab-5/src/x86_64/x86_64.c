#include "x86_64.h"
#include "util/log.h"
#include "util/macro.h"

#include <stddef.h>

x86_64_value __x86_64_make_void() { return (x86_64_value){.data = NULL}; }

static inline void __x86_64_op_base_error(const char *op) {
  error("basic implementation of operation %s is not defined", op);
}

x86_64_value __x86_64_op_base_plus(x86_64_value first) {
  UNUSED(first);
  __x86_64_op_base_error("plus");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_minus(x86_64_value first) {
  UNUSED(first);
  __x86_64_op_base_error("minus");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_not(x86_64_value first) {
  UNUSED(first);
  __x86_64_op_base_error("not");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_bit_not(x86_64_value first) {
  UNUSED(first);
  __x86_64_op_base_error("bit_not");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_inc(x86_64_value first) {
  UNUSED(first);
  __x86_64_op_base_error("inc");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_dec(x86_64_value first) {
  UNUSED(first);
  __x86_64_op_base_error("dec");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_or(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("or");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_and(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("and");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_bit_or(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("bit_or");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_bit_xor(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("bit_xor");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_bit_and(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("bit_and");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_eq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("eq");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_neq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("neq");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_less(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("less");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_less_eq(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("less_eq");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_bit_shl(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("bit_shl");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_bit_shr(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("bit_shr");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_add(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("add");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_mul(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("mul");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_div(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("div");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_rem(x86_64_value first, x86_64_value second) {
  UNUSED(first);
  UNUSED(second);
  __x86_64_op_base_error("rem");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_call(x86_64_value first, ...) {
  UNUSED(first);
  __x86_64_op_base_error("call");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_index(x86_64_value first, ...) {
  UNUSED(first);
  __x86_64_op_base_error("index");
  return __x86_64_make_void();
}

x86_64_value __x86_64_op_base_repr(x86_64_value first) {
  UNUSED(first);
  __x86_64_op_base_error("repr");
  return __x86_64_make_void();
}

void __x86_64_op_base_drop(x86_64_value *first) {
  UNUSED(first);
  __x86_64_op_base_error("drop");
}
