#include "bool.h"

void __x86_64_bool_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                        uint8_t value) {
  x86_64_data_bool data = {.value = value ? 1 : 0};
  __x86_64_value_init_raw(out, X86_64_TYPE_BOOL, op_tbl, *(uint64_t *)&data);
}
