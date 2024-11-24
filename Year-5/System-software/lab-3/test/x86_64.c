#include <criterion/criterion.h>
#include <stdint.h>

#include "codegen/x86_64_core.h"
#include "util/log.h"

Test(x86_64, sum_int) {
  cg_x86_64_value left;
  cg_x86_64_value right;
  cg_x86_64_value res;

  __cg_x86_64_init(&left);
  __cg_x86_64_init(&right);
  __cg_x86_64_init(&res);

  __cg_x86_64_drop(&left);
  left = __cg_x86_64_value_int(22);

  __cg_x86_64_drop(&right);
  right = __cg_x86_64_value_int(20);

  __cg_x86_64_drop(&res);
  res = __cg_x86_64_add(&left, &right);

  int32_t res_val = *res.data->data;
  cr_expect_eq(res_val, 42);

  __cg_x86_64_drop(&res);
  __cg_x86_64_drop(&left);
  __cg_x86_64_drop(&right);
}

Test(x86_64, sum_string) {
  cg_x86_64_value left;
  cg_x86_64_value right;
  cg_x86_64_value res;

  __cg_x86_64_init(&left);
  __cg_x86_64_init(&right);
  __cg_x86_64_init(&res);

  __cg_x86_64_drop(&left);
  left = __cg_x86_64_value_string("abcde");

  __cg_x86_64_drop(&right);
  right = __cg_x86_64_value_string("fghyu");

  __cg_x86_64_drop(&res);
  res = __cg_x86_64_add(&left, &right);

  char *res_val = res.data->data;
  debug("%zu, %s", res.data->data_size, res.data->data);
  cr_expect_eq(res.data->data_size, strlen("abcdefghyu") + 1);
  cr_expect_str_eq(res_val, "abcdefghyu");

  __cg_x86_64_drop(&res);
  __cg_x86_64_drop(&left);
  __cg_x86_64_drop(&right);
}
