#include "uint.h"

void __x86_64_uint_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                        uint32_t value) {
  x86_64_data_uint data = {.value = value};
  __x86_64_value_init_raw(out, X86_64_TYPE_UINT, op_tbl, *(uint64_t *)&data);
}
