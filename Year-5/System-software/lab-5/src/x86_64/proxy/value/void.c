#include "void.h"

#include "util/macro.h"
#include "x86_64/proxy/registry.h"
#include "x86_64/value.h"

void __x86_64_proxy_void_init(x86_64_value *out) {
  const x86_64_op_tbl *op_tbl = X86_64_REGISTRY.op_tbl_arr[X86_64_TYPE_VOID];

  out->type = X86_64_TYPE_VOID;
  out->op_tbl = (x86_64_op_tbl *)op_tbl;
}

void __x86_64_proxy_void_op_plus(x86_64_value *out, x86_64_value *self) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_minus(x86_64_value *out, x86_64_value *self) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_not(x86_64_value *out, x86_64_value *self) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_bit_not(x86_64_value *out, x86_64_value *self) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_inc(x86_64_value *out, x86_64_value *self) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_dec(x86_64_value *out, x86_64_value *self) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_or(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_and(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_bit_or(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_bit_xor(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_bit_and(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_eq(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_neq(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_less(x86_64_value *out, x86_64_value *self,
                                 x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_less_eq(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_bit_shl(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_bit_shr(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_add(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_sub(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_mul(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_div(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_rem(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(self);
  UNUSED(rsv);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_call(x86_64_value *out, x86_64_value *self, ...) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_index(x86_64_value *out, x86_64_value *self, ...) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

x86_64_op_repr __x86_64_proxy_void_op_repr;

void __x86_64_proxy_void_op_drop(x86_64_value *self) { UNUSED(self); }

void __x86_64_proxy_void_op_copy(x86_64_value *out, x86_64_value *self) {
  UNUSED(self);
  __x86_64_proxy_void_init(out);
}

void __x86_64_proxy_void_op_assign(x86_64_value *self, x86_64_value *other) {
  UNUSED(self);
  if (self == other) {
    return;
  }
  other->op_tbl->op_copy(self, other);
}

x86_64_op_assign_deref __x86_64_proxy_void_op_assign_deref;
