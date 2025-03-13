#include <criterion/criterion.h>
#include <stdint.h>

#include "x86_64/builtin/builtin.h"
#include "x86_64/value.h"

Test(x86_64_core_string, test1) {
  x86_64_value value;

  __x86_64_make_string(&value, (const uint8_t *)"string1");
  value.op_tbl->op_drop(&value);
}

Test(x86_64_core_string, test2) {
  x86_64_value value1;
  x86_64_value value2;

  __x86_64_make_string(&value1, (const uint8_t *)"string1");
  __x86_64_make_string(&value2, (const uint8_t *)"string2");

  value2.op_tbl->op_assign(&value2, &value1);

  value1.op_tbl->op_drop(&value1);
  value2.op_tbl->op_drop(&value2);
}
