#include <criterion/criterion.h>
#include <stdint.h>

#include "util/macro.h"

#include "x86_64/builtin/builtin.h"
#include "x86_64/value.h"

Test(x86_64_core, base_op_tbl) {
  x86_64_value value;
  UNUSED(value);

  value = __x86_64_make_void();
  value.data->op_tbl->op_repr(value);
  value.data->op_tbl->op_drop(&value);
}
