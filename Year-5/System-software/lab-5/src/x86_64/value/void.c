#include "void.h"

void __x86_64_void_init(x86_64_value *value, x86_64_op_tbl *op_tbl) {
  value->type = X86_64_TYPE_VOID;
  value->op_tbl = op_tbl;
}
