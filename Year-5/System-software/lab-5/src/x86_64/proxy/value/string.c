#include "string.h"

#include "util/macro.h"
#include "x86_64/proxy/registry.h"
#include "x86_64/proxy/util.h"
#include "x86_64/proxy/value/void.h"
#include "x86_64/value.h"
#include "x86_64/value/string.h"
#include <stdlib.h>
#include <string.h>

void __x86_64_proxy_string_init(x86_64_value *out, const uint8_t *value) {
  x86_64_data_string data = {.value = (uint8_t *)strdup((const char *)value)};

  const x86_64_op_tbl *op_tbl = X86_64_REGISTRY.op_tbl_arr[X86_64_TYPE_STRING];

  __x86_64_value_init_raw(out, X86_64_TYPE_STRING, (x86_64_op_tbl *)op_tbl,
                          *(uint64_t *)&data);
}

void __x86_64_proxy_string_op_plus(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "plus");
}

void __x86_64_proxy_string_op_minus(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "minus");
}

void __x86_64_proxy_string_op_not(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "not");
}

void __x86_64_proxy_string_op_bit_not(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "bit_not");
}

void __x86_64_proxy_string_op_inc(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "inc");
}

void __x86_64_proxy_string_op_dec(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "dec");
}

void __x86_64_proxy_string_op_or(x86_64_value *out, x86_64_value *self,
                                 x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "or");
}

void __x86_64_proxy_string_op_and(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "and");
}

void __x86_64_proxy_string_op_bit_or(x86_64_value *out, x86_64_value *self,
                                     x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_or");
}

void __x86_64_proxy_string_op_bit_xor(x86_64_value *out, x86_64_value *self,
                                      x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_xor");
}

void __x86_64_proxy_string_op_bit_and(x86_64_value *out, x86_64_value *self,
                                      x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_and");
}

void __x86_64_proxy_string_op_eq(x86_64_value *out, x86_64_value *self,
                                 x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "eq");
}

void __x86_64_proxy_string_op_neq(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "neq");
}

void __x86_64_proxy_string_op_less(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "less");
}

void __x86_64_proxy_string_op_less_eq(x86_64_value *out, x86_64_value *self,
                                      x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "less_eq");
}

void __x86_64_proxy_string_op_bit_shl(x86_64_value *out, x86_64_value *self,
                                      x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_shl");
}

void __x86_64_proxy_string_op_bit_shr(x86_64_value *out, x86_64_value *self,
                                      x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_shr");
}

void __x86_64_proxy_string_op_add(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {

  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "add", self->type);
    return;
  }

  x86_64_data_string self_data = *(x86_64_data_string *)&self->data_raw;
  x86_64_data_string rsv_data = *(x86_64_data_string *)&rsv->data_raw;

  size_t self_size = strlen((char *)self_data.value);
  size_t rsv_size = strlen((char *)rsv_data.value);

  x86_64_data_string data = {.value =
                                 (uint8_t *)malloc(self_size + rsv_size + 1)};

  memcpy(data.value, self_data.value, self_size);
  memcpy(data.value + self_size, rsv_data.value, rsv_size);
  data.value[self_size + rsv_size] = '\0';

  const x86_64_op_tbl *op_tbl = X86_64_REGISTRY.op_tbl_arr[X86_64_TYPE_STRING];

  __x86_64_value_init_raw(out, X86_64_TYPE_STRING, (x86_64_op_tbl *)op_tbl,
                          *(uint64_t *)&data);
}

void __x86_64_proxy_string_op_sub(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "sub");
}

void __x86_64_proxy_string_op_mul(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "mul");
}

void __x86_64_proxy_string_op_div(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "div");
}

void __x86_64_proxy_string_op_rem(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "rem");
}

void __x86_64_proxy_string_op_call(x86_64_value *out, x86_64_value *self, ...) {
  __x86_64_proxy_op_undefined(out, self, "call");
}

x86_64_op_index __x86_64_proxy_string_op_index;

void __x86_64_proxy_string_op_repr(x86_64_value *out, x86_64_value *self) {
  self->op_tbl->op_copy(out, self);
}

void __x86_64_proxy_string_op_drop(x86_64_value *self) {
  x86_64_data_string data = *(x86_64_data_string *)&self->data_raw;
  free(data.value);
  __x86_64_proxy_void_init(self);
}

void __x86_64_proxy_string_op_copy(x86_64_value *out, x86_64_value *self) {
  x86_64_data_string data = *(x86_64_data_string *)&self->data_raw;
  __x86_64_proxy_string_init(out, data.value);
}

void __x86_64_proxy_string_op_assign(x86_64_value *self, x86_64_value *other) {
  if (self == other) {
    return;
  }
  self->op_tbl->op_drop(self);
  other->op_tbl->op_copy(self, other);
}

// void __x86_64_proxy_string_op_assign_deref(x86_64_value *self,
//                                            x86_64_value *other) {
//   UNUSED(other);

//   x86_64_value out;
//   __x86_64_proxy_op_undefined(&out, self, "assign_deref");
//   self->op_tbl->op_drop(self);
//   *self = out;
// }
