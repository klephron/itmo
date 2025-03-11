#include <criterion/criterion.h>
#include <stdint.h>

#include "util/macro.h"
#include "x86_64/x86_64.h"

Test(x86_64_core, base_op_tbl) {
  x86_64_value value;
  UNUSED(value);

  value = __x86_64_make_void();
}
