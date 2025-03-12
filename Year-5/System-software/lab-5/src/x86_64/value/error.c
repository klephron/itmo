#include "error.h"
#include <stdlib.h>

void __x86_64_error_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                         x86_64_value *value) {
  x86_64_data_error *data = malloc(sizeof(x86_64_data_error));
  data->ref_cnt = 1;
  data->value = *value;

  __x86_64_value_init_ptr(out, X86_64_TYPE_ERROR, op_tbl, data);
}
