#include "string.h"

#include <string.h>

void __x86_64_string_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                          const uint8_t *value) {
  __x86_64_value_init_ptr(out, X86_64_TYPE_STRING, op_tbl,
                          strdup((const char *)value));
}
