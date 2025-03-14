#include "int.h"

#include "util/macro.h"
#include "x86_64/proxy/registry.h"
#include "x86_64/proxy/util.h"
#include "x86_64/proxy/value/bool.h"
#include "x86_64/proxy/value/string.h"
#include "x86_64/proxy/value/void.h"
#include "x86_64/value/int.h"
#include <stdio.h>

void __x86_64_proxy_int_init(x86_64_value *out, int32_t value) {
  x86_64_data_int data = {.value = value};

  const x86_64_op_tbl *op_tbl = X86_64_REGISTRY.op_tbl_arr[X86_64_TYPE_INT];

  __x86_64_value_init_raw(out, X86_64_TYPE_INT, (x86_64_op_tbl *)op_tbl,
                          *(uint64_t *)&data);
}

void __x86_64_proxy_int_op_plus(x86_64_value *out, x86_64_value *self) {
  self->op_tbl->op_copy(out, self);
}

void __x86_64_proxy_int_op_minus(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "minus");
}

void __x86_64_proxy_int_op_not(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "not");
}

void __x86_64_proxy_int_op_bit_not(x86_64_value *out, x86_64_value *self) {
  x86_64_data_int data = *(x86_64_data_int *)&self->data_raw;
  __x86_64_proxy_int_init(out, ~data.value);
}

void __x86_64_proxy_int_op_inc(x86_64_value *out, x86_64_value *self) {
  x86_64_data_int data = *(x86_64_data_int *)&self->data_raw;
  __x86_64_proxy_int_init(out, data.value + 1);
}

void __x86_64_proxy_int_op_dec(x86_64_value *out, x86_64_value *self) {
  x86_64_data_int data = *(x86_64_data_int *)&self->data_raw;
  __x86_64_proxy_int_init(out, data.value - 1);
}

void __x86_64_proxy_int_op_or(x86_64_value *out, x86_64_value *self,
                              x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "or");
}

void __x86_64_proxy_int_op_and(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "and");
}

void __x86_64_proxy_int_op_bit_or(x86_64_value *out, x86_64_value *self,
                                  x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "bit_or", self->type);
    __x86_64_proxy_void_init(out);
  }

  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value | rsv_data.value);
}

void __x86_64_proxy_int_op_bit_xor(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "bit_xor", self->type);
    __x86_64_proxy_void_init(out);
  }

  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value ^ rsv_data.value);
}

void __x86_64_proxy_int_op_bit_and(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "bit_and", self->type);
    __x86_64_proxy_void_init(out);
  }

  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value & rsv_data.value);
}

void __x86_64_proxy_int_op_eq(x86_64_value *out, x86_64_value *self,
                              x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "eq", self->type);
    __x86_64_proxy_void_init(out);
  }

  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value == rsv_data.value);
}

void __x86_64_proxy_int_op_neq(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "neq", self->type);
    __x86_64_proxy_void_init(out);
  }

  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value != rsv_data.value);
}

void __x86_64_proxy_int_op_less(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "less", self->type);
    __x86_64_proxy_void_init(out);
  }

  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value < rsv_data.value);
}

void __x86_64_proxy_int_op_less_eq(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "less_eq", self->type);
    __x86_64_proxy_void_init(out);
  }

  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value <= rsv_data.value);
}

void __x86_64_proxy_int_op_bit_shl(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "bit_shl", self->type);
    __x86_64_proxy_void_init(out);
  }
  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value << rsv_data.value);
}

void __x86_64_proxy_int_op_bit_shr(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "bit_shr", self->type);
    __x86_64_proxy_void_init(out);
  }
  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value >> rsv_data.value);
}

void __x86_64_proxy_int_op_add(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "add", self->type);
    __x86_64_proxy_void_init(out);
  }
  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value + rsv_data.value);
}

void __x86_64_proxy_int_op_sub(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "sub", self->type);
    __x86_64_proxy_void_init(out);
  }
  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value - rsv_data.value);
}

void __x86_64_proxy_int_op_mul(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "mul", self->type);
    __x86_64_proxy_void_init(out);
  }
  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value * rsv_data.value);
}

void __x86_64_proxy_int_op_div(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "div", self->type);
    __x86_64_proxy_void_init(out);
  }
  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value / rsv_data.value);
}

void __x86_64_proxy_int_op_rem(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "rem", self->type);
    __x86_64_proxy_void_init(out);
  }
  x86_64_data_int self_data = *(x86_64_data_int *)&self->data_raw;
  x86_64_data_int rsv_data = *(x86_64_data_int *)&rsv->data_raw;

  __x86_64_proxy_int_init(out, self_data.value % rsv_data.value);
}

void __x86_64_proxy_int_op_call(x86_64_value *out, x86_64_value *self, ...) {
  __x86_64_proxy_op_undefined(out, self, "call");
}

void __x86_64_proxy_int_op_index(x86_64_value *out, x86_64_value *self, ...) {
  __x86_64_proxy_op_undefined(out, self, "index");
}

void __x86_64_proxy_int_op_repr(x86_64_value *out, x86_64_value *self) {
  uint8_t buf[16];

  x86_64_data_int data = *(x86_64_data_int *)&self->data_raw;
  snprintf((char *)buf, STRMAXLEN(buf), "%d", data.value);
  __x86_64_proxy_string_init(out, buf);
}

void __x86_64_proxy_int_op_drop(x86_64_value *self) {
  __x86_64_proxy_void_init(self);
}

void __x86_64_proxy_int_op_copy(x86_64_value *out, x86_64_value *self) {
  x86_64_data_int data = *(x86_64_data_int *)&self->data_raw;
  __x86_64_proxy_int_init(out, data.value);
}

void __x86_64_proxy_int_op_assign(x86_64_value *self, x86_64_value *other) {
  other->op_tbl->op_copy(self, other);
}

// void __x86_64_proxy_int_op_assign_deref(x86_64_value *self,
//                                          x86_64_value *other) {
//   UNUSED(other);
//   __x86_64_proxy_op_undefined(self, self, "assign_deref");
// }
