#include "array.h"
#include "x86_64/proxy/data/void.h"
#include <stdlib.h>

void __x86_64_array_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                         uint64_t length) {
  x86_64_data_array *data =
      malloc(sizeof(x86_64_data_array) + length * sizeof(x86_64_value));

  data->ref_cnt = 1;
  data->length = length;
  for (uint64_t i = 0; i < length; ++i) {
    __x86_64_proxy_void_init(data->elements + i);
  }

  __x86_64_value_init_ptr(out, X86_64_TYPE_ARRAY, op_tbl, data);
}
