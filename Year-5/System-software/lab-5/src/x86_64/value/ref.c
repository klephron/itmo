#include "ref.h"

void __x86_64_ref_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                       x86_64_value *value) {
  __x86_64_value_init_ptr(out, X86_64_TYPE_REF, op_tbl, value);
}
