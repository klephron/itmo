#include <criterion/criterion.h>
#include <stdint.h>

#include "util/log.h"
#include "x86_64/builtin/builtin.h"
#include "x86_64/value.h"

Test(x86_64_make, test1_index) {
  x86_64_value value1;
  x86_64_value value2;
  x86_64_value value3;
  x86_64_value value4;
  x86_64_value value5;

  __x86_64_make_int(&value1, 3);
  __x86_64_make_int(&value2, 4);
  __x86_64_make_int(&value3, 5);

  __x86_64_make_array(&value4, &value1, &value2, &value3, NULL);

  value4.op_tbl->op_repr(&value5, &value4);
  debug("make_array: %s", value5.data_ptr);
  value5.op_tbl->op_drop(&value5);

  value4.op_tbl->op_drop(&value4);
  value3.op_tbl->op_drop(&value3);
  value2.op_tbl->op_drop(&value2);
  value1.op_tbl->op_drop(&value1);
}
