#include "builtin.h"
#include "x86_64_core/util.h"

#include <stdio.h>

void __x86_64_print(x86_64_value *self) {
  x86_64_value self_repr;
  self->op_tbl->op_repr(&self_repr, self);

  fprintf(stdout, "%s: %s\n", (char *)self_repr.data_ptr,
          __x86_64_type_enum_str(self->type));

  self_repr.op_tbl->op_drop(&self_repr);
}
