#include <criterion/criterion.h>
#include <stdint.h>

#include "util/log.h"
#include "x86_64/proxy/value/bool.h"
#include "x86_64/value.h"

Test(x86_64_core_bool, test1) {
  x86_64_value value1;
  x86_64_value value2;
  x86_64_value value3;
  x86_64_value value4;

  __x86_64_proxy_bool_init(&value1, 'a');

  // repr
  value1.op_tbl->op_repr(&value2, &value1);
  debug("bool_repr: %s", value2.data_ptr);
  value2.op_tbl->op_drop(&value2);

  // plus
  value1.op_tbl->op_plus(&value2, &value1);

  value2.op_tbl->op_repr(&value3, &value2);
  debug("bool_plus_repr: %s", value3.data_ptr);
  value3.op_tbl->op_drop(&value3);
  value2.op_tbl->op_drop(&value2);

  // inc
  value1.op_tbl->op_inc(&value2, &value1);

  value2.op_tbl->op_repr(&value3, &value2);
  debug("bool_inc_repr: %s", value3.data_ptr);
  value3.op_tbl->op_drop(&value3);
  value2.op_tbl->op_drop(&value2);

  // dec
  value1.op_tbl->op_dec(&value2, &value1);

  value2.op_tbl->op_repr(&value3, &value2);
  debug("bool_dec_repr: %s", value3.data_ptr);
  value3.op_tbl->op_drop(&value3);
  value2.op_tbl->op_drop(&value2);

  // add
  __x86_64_proxy_bool_init(&value4, 200);

  value4.op_tbl->op_add(&value2, &value4, &value1);

  value2.op_tbl->op_repr(&value3, &value2);
  debug("bool_add_repr: %s", value3.data_ptr);
  value3.op_tbl->op_drop(&value3);
  value2.op_tbl->op_drop(&value2);

  value1.op_tbl->op_drop(&value1);
  value4.op_tbl->op_drop(&value4);
}
