#include "string.h"

#include "x86_64/proxy/registry.h"
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

x86_64_op_plus    __x86_64_proxy_string_op_plus;
x86_64_op_minus   __x86_64_proxy_string_op_minus;
x86_64_op_not     __x86_64_proxy_string_op_not;
x86_64_op_bit_not __x86_64_proxy_string_op_bit_not;
x86_64_op_inc     __x86_64_proxy_string_op_inc;
x86_64_op_dec     __x86_64_proxy_string_op_dec;
x86_64_op_or      __x86_64_proxy_string_op_or;
x86_64_op_and     __x86_64_proxy_string_op_and;
x86_64_op_bit_or  __x86_64_proxy_string_op_bit_or;
x86_64_op_bit_xor __x86_64_proxy_string_op_bit_xor;
x86_64_op_bit_and __x86_64_proxy_string_op_bit_and;
x86_64_op_eq      __x86_64_proxy_string_op_eq;
x86_64_op_neq     __x86_64_proxy_string_op_neq;
x86_64_op_less    __x86_64_proxy_string_op_less;
x86_64_op_less_eq __x86_64_proxy_string_op_less_eq;
x86_64_op_bit_shl __x86_64_proxy_string_op_bit_shl;
x86_64_op_bit_shr __x86_64_proxy_string_op_bit_shr;
x86_64_op_add     __x86_64_proxy_string_op_add;
x86_64_op_sub     __x86_64_proxy_string_op_sub;
x86_64_op_mul     __x86_64_proxy_string_op_mul;
x86_64_op_div     __x86_64_proxy_string_op_div;
x86_64_op_rem     __x86_64_proxy_string_op_rem;
x86_64_op_call    __x86_64_proxy_string_op_call;
x86_64_op_index   __x86_64_proxy_string_op_index;

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

x86_64_op_assign_deref __x86_64_proxy_string_op_assign_deref;
