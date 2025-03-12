#include "int.h"

void __x86_64_int_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                       int32_t value) {
  x86_64_data_int data = {.value = value};
  __x86_64_value_init_raw(out, X86_64_TYPE_INT, op_tbl, *(uint64_t *)&data);
}
