#include <criterion/criterion.h>
#include <stdint.h>

#include "codegen/x86_64_core.h"
#include "util/log.h"

Test(x86_64, sum_int) {
  cg_x86_64_value left;
  cg_x86_64_value right;
  cg_x86_64_value result;

  __cg_x86_64_init(&left);
  __cg_x86_64_init(&right);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&left);
  left = __cg_x86_64_value_int(22);

  __cg_x86_64_drop(&right);
  right = __cg_x86_64_value_int(20);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_add(&left, &right);

  int32_t res = *result.data->raw;
  cr_expect_eq(res, 42);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&left);
  __cg_x86_64_drop(&right);
}

Test(x86_64, sum_string) {
  cg_x86_64_value left;
  cg_x86_64_value right;
  cg_x86_64_value result;

  __cg_x86_64_init(&left);
  __cg_x86_64_init(&right);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&left);
  left = __cg_x86_64_value_string((const uint8_t *)"abcde");

  __cg_x86_64_drop(&right);
  right = __cg_x86_64_value_string((const uint8_t *)"fghyu");

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_add(&left, &right);

  uint8_t *res = result.data->raw;
  debug("%zu, %s", result.data->data_size, result.data->raw);
  cr_expect_eq(result.data->data_size, strlen("abcdefghyu") + 1);
  cr_expect_str_eq((char *)res, "abcdefghyu");

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&left);
  __cg_x86_64_drop(&right);
}

Test(x86_64, array_create) {
  cg_x86_64_value first;
  cg_x86_64_value second;
  cg_x86_64_value result;

  __cg_x86_64_init(&first);
  __cg_x86_64_init(&second);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&first);
  first = __cg_x86_64_value_long(10);

  __cg_x86_64_drop(&second);
  second = __cg_x86_64_value_long(8);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_add(&first, &second);
  debug("res: %zu, %ld", result.data->data_size, *result.data->raw);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_value_array(
      &(cg_x86_64_type){.kind = CG_X86_64_TYPE_INT}, 2, &first, &second);

  cr_expect_eq(80 * 4, result.data->data_size);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&first);
  __cg_x86_64_drop(&second);
}

Test(x86_64, string_index) {
  cg_x86_64_value string;
  cg_x86_64_value index;
  cg_x86_64_value result;

  __cg_x86_64_init(&string);
  __cg_x86_64_init(&index);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&string);
  string = __cg_x86_64_value_string((const uint8_t *)"abcde");

  __cg_x86_64_drop(&index);
  index = __cg_x86_64_value_long(3);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_index(&string, 1, &index);
  debug("res: %zu, %c", result.data->data_size, *result.data->raw);

  cr_expect_eq('d', (char)*result.data->raw);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&string);
  __cg_x86_64_drop(&index);
}

Test(x86_64, array_index_1d) {
  cg_x86_64_value array;
  cg_x86_64_value rank_1;
  cg_x86_64_value index;
  cg_x86_64_value result;

  __cg_x86_64_init(&array);
  __cg_x86_64_init(&rank_1);
  __cg_x86_64_init(&index);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&rank_1);
  rank_1 = __cg_x86_64_value_long(10);

  __cg_x86_64_drop(&index);
  index = __cg_x86_64_value_long(1);

  __cg_x86_64_drop(&array);
  array = __cg_x86_64_value_array(
      &(cg_x86_64_type){.kind = CG_X86_64_TYPE_LONG}, 1, &rank_1);

  result = __cg_x86_64_index(&array, 1, &index);
  debug("res: %zu, %c", result.data->data_size, *result.data->raw);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&index);
  __cg_x86_64_drop(&rank_1);
  __cg_x86_64_drop(&array);
}

Test(x86_64, string_set) {
  cg_x86_64_value string;
  cg_x86_64_value index;
  cg_x86_64_value chr;

  __cg_x86_64_init(&string);
  __cg_x86_64_init(&index);
  __cg_x86_64_init(&chr);

  __cg_x86_64_drop(&string);
  string = __cg_x86_64_value_string((const uint8_t *)"abcdef");

  __cg_x86_64_drop(&index);
  index = __cg_x86_64_value_long(1);

  __cg_x86_64_drop(&chr);
  chr = __cg_x86_64_value_char('x');

  __cg_x86_64_index_set(&chr, &string, 1, &index);

  cr_expect_eq('x', string.data->raw[1]);

  __cg_x86_64_drop(&chr);
  __cg_x86_64_drop(&index);
  __cg_x86_64_drop(&string);
}

Test(x86_64, array_set_1d) {
  cg_x86_64_value array;
  cg_x86_64_value rank_1;
  cg_x86_64_value index_1;
  cg_x86_64_value result;

  __cg_x86_64_init(&array);
  __cg_x86_64_init(&rank_1);
  __cg_x86_64_init(&index_1);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&rank_1);
  rank_1 = __cg_x86_64_value_long(10);

  __cg_x86_64_drop(&index_1);
  index_1 = __cg_x86_64_value_long(1);

  __cg_x86_64_drop(&array);
  array = __cg_x86_64_value_array(
      &(cg_x86_64_type){.kind = CG_X86_64_TYPE_LONG}, 1, &rank_1);

  __cg_x86_64_index_set(&rank_1, &array, 1, &index_1);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_index(&array, 1, &index_1);

  int64_t res = *result.data->raw;
  cr_expect_eq(10, res);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&index_1);
  __cg_x86_64_drop(&rank_1);
  __cg_x86_64_drop(&array);
}

Test(x86_64, array_set_2d) {
  cg_x86_64_value array;
  cg_x86_64_value rank_1;
  cg_x86_64_value index_1;
  cg_x86_64_value index_2;
  cg_x86_64_value result;

  __cg_x86_64_init(&array);
  __cg_x86_64_init(&rank_1);
  __cg_x86_64_init(&index_1);
  __cg_x86_64_init(&index_2);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&rank_1);
  rank_1 = __cg_x86_64_value_long(10);

  __cg_x86_64_drop(&index_1);
  index_1 = __cg_x86_64_value_long(1);
  __cg_x86_64_drop(&index_2);
  index_2 = __cg_x86_64_value_long(2);

  __cg_x86_64_drop(&array);
  array = __cg_x86_64_value_array(
      &(cg_x86_64_type){.kind = CG_X86_64_TYPE_LONG}, 2, &rank_1, &rank_1);

  __cg_x86_64_index_set(&rank_1, &array, 2, &index_1, &index_2);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_index(&array, 2, &index_1, &index_2);

  int64_t res = *result.data->raw;
  cr_expect_eq(10, res);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&index_1);
  __cg_x86_64_drop(&index_2);
  __cg_x86_64_drop(&rank_1);
  __cg_x86_64_drop(&array);
}

Test(x86_64, array_cast_1) {
  cg_x86_64_value source;
  cg_x86_64_value target;

  __cg_x86_64_init(&source);
  __cg_x86_64_init(&target);

  __cg_x86_64_drop(&source);
  source = __cg_x86_64_value_long(10);

  __cg_x86_64_drop(&target);
  target = __cg_x86_64_cast(&source, CG_X86_64_TYPE_BYTE);

  int64_t res = *target.data->raw;
  cr_expect_eq(10, res);

  __cg_x86_64_drop(&source);
  __cg_x86_64_drop(&target);
}

Test(x86_64, array_cast_2) {
  cg_x86_64_value source;
  cg_x86_64_value target;

  __cg_x86_64_init(&source);
  __cg_x86_64_init(&target);

  __cg_x86_64_drop(&source);
  source = __cg_x86_64_value_uint(10);

  __cg_x86_64_drop(&target);
  target = __cg_x86_64_cast(&source, CG_X86_64_TYPE_LONG);

  uint32_t res = *target.data->raw;
  cr_expect_eq(10, res);

  __cg_x86_64_drop(&source);
  __cg_x86_64_drop(&target);
}

Test(x86_64, long_shl) {
  cg_x86_64_value first;
  cg_x86_64_value second;
  cg_x86_64_value result;

  __cg_x86_64_init(&first);
  __cg_x86_64_init(&second);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&first);
  first = __cg_x86_64_value_long(1);

  __cg_x86_64_drop(&second);
  second = __cg_x86_64_value_long(3);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_bit_shl(&first, &second);

  int64_t res = *result.data->raw;
  cr_expect_eq(8, res);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&second);
  __cg_x86_64_drop(&first);
}

Test(x86_64, bool_eq_neq) {
  cg_x86_64_value first;
  cg_x86_64_value second;
  cg_x86_64_value result;
  int8_t          res;

  __cg_x86_64_init(&first);
  __cg_x86_64_init(&second);
  __cg_x86_64_init(&result);

  __cg_x86_64_drop(&first);
  first = __cg_x86_64_value_bool(true);

  __cg_x86_64_drop(&second);
  second = __cg_x86_64_value_bool(false);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_eq(&first, &second);

  res = *result.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(0, res);

  __cg_x86_64_drop(&result);
  result = __cg_x86_64_neq(&first, &second);

  res = *result.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_neq(0, res);

  __cg_x86_64_drop(&result);
  __cg_x86_64_drop(&second);
  __cg_x86_64_drop(&first);
}

Test(x86_64, bool_not) {
  cg_x86_64_value first;
  cg_x86_64_value second;
  int8_t          res;

  __cg_x86_64_init(&first);
  __cg_x86_64_init(&second);

  __cg_x86_64_drop(&first);
  first = __cg_x86_64_value_bool(true);

  res = *first.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(1, res);

  __cg_x86_64_drop(&second);
  second = __cg_x86_64_not(&first);

  res = *second.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(0, res);

  __cg_x86_64_drop(&first);
  first = __cg_x86_64_not(&second);

  res = *first.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(1, res);

  __cg_x86_64_drop(&second);
  __cg_x86_64_drop(&first);
}
