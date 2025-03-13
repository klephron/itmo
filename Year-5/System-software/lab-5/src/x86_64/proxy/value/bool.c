#include "bool.h"

#include "util/macro.h"
#include "x86_64/proxy/registry.h"
#include "x86_64/proxy/util.h"
#include "x86_64/proxy/value/string.h"
#include "x86_64/proxy/value/void.h"
#include "x86_64/value/bool.h"

void __x86_64_proxy_bool_init(x86_64_value *out, uint8_t value) {
  x86_64_data_bool data = {.value = value ? 1 : 0};

  const x86_64_op_tbl *op_tbl = X86_64_REGISTRY.op_tbl_arr[X86_64_TYPE_BOOL];

  __x86_64_value_init_raw(out, X86_64_TYPE_BOOL, (x86_64_op_tbl *)op_tbl,
                          *(uint64_t *)&data);
}

void __x86_64_proxy_bool_op_plus(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "plus");
}

void __x86_64_proxy_bool_op_minus(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "minus");
}

void __x86_64_proxy_bool_op_not(x86_64_value *out, x86_64_value *self) {
  x86_64_data_bool data = *(x86_64_data_bool *)&self->data_raw;
  __x86_64_proxy_bool_init(out, !data.value);
}

void __x86_64_proxy_bool_op_bit_not(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "not");
}

void __x86_64_proxy_bool_op_inc(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "inc");
}

void __x86_64_proxy_bool_op_dec(x86_64_value *out, x86_64_value *self) {
  __x86_64_proxy_op_undefined(out, self, "dec");
}

void __x86_64_proxy_bool_op_or(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "or", self->type);
    __x86_64_proxy_void_init(out);
    return;
  }

  x86_64_data_bool self_data = *(x86_64_data_bool *)&self->data_raw;
  x86_64_data_bool rsv_data = *(x86_64_data_bool *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value || rsv_data.value);
}

void __x86_64_proxy_bool_op_and(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "and", self->type);
    __x86_64_proxy_void_init(out);
    return;
  }

  x86_64_data_bool self_data = *(x86_64_data_bool *)&self->data_raw;
  x86_64_data_bool rsv_data = *(x86_64_data_bool *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value && rsv_data.value);
}

void __x86_64_proxy_bool_op_bit_or(x86_64_value *out, x86_64_value *self,
                                   x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_or");
}

void __x86_64_proxy_bool_op_bit_xor(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_xor");
}

void __x86_64_proxy_bool_op_bit_and(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_and");
}

void __x86_64_proxy_bool_op_eq(x86_64_value *out, x86_64_value *self,
                               x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "eq", self->type);
    __x86_64_proxy_void_init(out);
    return;
  }

  x86_64_data_bool self_data = *(x86_64_data_bool *)&self->data_raw;
  x86_64_data_bool rsv_data = *(x86_64_data_bool *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value == rsv_data.value);
}

void __x86_64_proxy_bool_op_neq(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  if (self->type != rsv->type) {
    __x86_64_proxy_op_type_mismatch(out, rsv, "neq", self->type);
    __x86_64_proxy_void_init(out);
    return;
  }

  x86_64_data_bool self_data = *(x86_64_data_bool *)&self->data_raw;
  x86_64_data_bool rsv_data = *(x86_64_data_bool *)&rsv->data_raw;

  __x86_64_proxy_bool_init(out, self_data.value != rsv_data.value);
}

void __x86_64_proxy_bool_op_less(x86_64_value *out, x86_64_value *self,
                                 x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "less");
}

void __x86_64_proxy_bool_op_less_eq(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "less_eq");
}

void __x86_64_proxy_bool_op_bit_shl(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_shl");
}

void __x86_64_proxy_bool_op_bit_shr(x86_64_value *out, x86_64_value *self,
                                    x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "bit_shr");
}

void __x86_64_proxy_bool_op_add(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "add");
}

void __x86_64_proxy_bool_op_sub(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "sub");
}

void __x86_64_proxy_bool_op_mul(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "mul");
}

void __x86_64_proxy_bool_op_div(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "div");
}

void __x86_64_proxy_bool_op_rem(x86_64_value *out, x86_64_value *self,
                                x86_64_value *rsv) {
  UNUSED(rsv);
  __x86_64_proxy_op_undefined(out, self, "rem");
}

void __x86_64_proxy_bool_op_call(x86_64_value *out, x86_64_value *self, ...) {
  __x86_64_proxy_op_undefined(out, self, "call");
}

void __x86_64_proxy_bool_op_index(x86_64_value *out, x86_64_value *self, ...) {
  __x86_64_proxy_op_undefined(out, self, "index");
}

void __x86_64_proxy_bool_op_repr(x86_64_value *out, x86_64_value *self) {
  x86_64_data_bool data = *(x86_64_data_bool *)&self->data_raw;
  if (data.value) {
    __x86_64_proxy_string_init(out, (const uint8_t *)"true");
  } else {
    __x86_64_proxy_string_init(out, (const uint8_t *)"false");
  }
}

void __x86_64_proxy_bool_op_drop(x86_64_value *self) {
  __x86_64_proxy_void_init(self);
}

void __x86_64_proxy_bool_op_copy(x86_64_value *out, x86_64_value *self) {
  x86_64_data_bool data = *(x86_64_data_bool *)&self->data_raw;
  __x86_64_proxy_bool_init(out, data.value);
}

void __x86_64_proxy_bool_op_assign(x86_64_value *self, x86_64_value *other) {
  other->op_tbl->op_copy(self, other);
}

void __x86_64_proxy_bool_op_assign_deref(x86_64_value *self,
                                         x86_64_value *other) {
  UNUSED(other);
  __x86_64_proxy_op_undefined(self, self, "assign_deref");
}
