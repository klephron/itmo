#include "object.h"

#include "x86_64/proxy/registry.h"
#include "x86_64/proxy/value/void.h"
#include <stdlib.h>

void __x86_64_proxy_object_init(x86_64_value                     *out,
                                const x86_64_data_object_symbols *symbols) {

  x86_64_data_object *data = malloc(sizeof(x86_64_data_object) +
                                    symbols->count * sizeof(x86_64_value));
  data->symbols_ref = symbols;
  for (uint64_t i = 0; i < symbols->count; ++i) {
    __x86_64_proxy_void_init(data->members + i);
  }

  const x86_64_op_tbl *op_tbl = X86_64_REGISTRY.op_tbl_arr[X86_64_TYPE_OBJECT];

  __x86_64_value_init_ptr(out, X86_64_TYPE_OBJECT, (x86_64_op_tbl *)op_tbl,
                          data);
}

x86_64_op_plus    __x86_64_proxy_object_op_plus;
x86_64_op_minus   __x86_64_proxy_object_op_minus;
x86_64_op_not     __x86_64_proxy_object_op_not;
x86_64_op_bit_not __x86_64_proxy_object_op_bit_not;
x86_64_op_inc     __x86_64_proxy_object_op_inc;
x86_64_op_dec     __x86_64_proxy_object_op_dec;
x86_64_op_or      __x86_64_proxy_object_op_or;
x86_64_op_and     __x86_64_proxy_object_op_and;
x86_64_op_bit_or  __x86_64_proxy_object_op_bit_or;
x86_64_op_bit_xor __x86_64_proxy_object_op_bit_xor;
x86_64_op_bit_and __x86_64_proxy_object_op_bit_and;
x86_64_op_eq      __x86_64_proxy_object_op_eq;
x86_64_op_neq     __x86_64_proxy_object_op_neq;
x86_64_op_less    __x86_64_proxy_object_op_less;
x86_64_op_less_eq __x86_64_proxy_object_op_less_eq;
x86_64_op_bit_shl __x86_64_proxy_object_op_bit_shl;
x86_64_op_bit_shr __x86_64_proxy_object_op_bit_shr;
x86_64_op_add     __x86_64_proxy_object_op_add;
x86_64_op_sub     __x86_64_proxy_object_op_sub;
x86_64_op_mul     __x86_64_proxy_object_op_mul;
x86_64_op_div     __x86_64_proxy_object_op_div;
x86_64_op_rem     __x86_64_proxy_object_op_rem;
x86_64_op_call    __x86_64_proxy_object_op_call;
x86_64_op_index   __x86_64_proxy_object_op_index;
x86_64_op_repr    __x86_64_proxy_object_op_repr;
x86_64_op_drop    __x86_64_proxy_object_op_drop;
x86_64_op_copy    __x86_64_proxy_object_op_copy;
x86_64_op_assign  __x86_64_proxy_object_op_assign;
// x86_64_op_assign_deref __x86_64_proxy_object_op_assign_deref;
