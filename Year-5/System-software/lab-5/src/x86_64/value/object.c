#include "object.h"

#include "x86_64/proxy/data/void.h"
#include <stdlib.h>

void __x86_64_object_init(x86_64_value *out, x86_64_op_tbl *op_tbl,
                          const x86_64_data_object_symbols *symbols) {
  x86_64_data_object *data = malloc(sizeof(x86_64_data_object) +
                                    symbols->count * sizeof(x86_64_value));
  data->symbols_ref = symbols;
  for (uint64_t i = 0; i < symbols->count; ++i) {
    __x86_64_proxy_void_init(data->members + i);
  }

  __x86_64_value_init_ptr(out, X86_64_TYPE_OBJECT, op_tbl, data);
}
