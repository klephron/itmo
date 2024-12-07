#include <criterion/criterion.h>
#include <stdint.h>

#include "util/log.h"
#include "x86_64_core/x86_64_core.h"

Test(x86_64_core, sum_int) {
  x86_64_value left;
  x86_64_value right;
  x86_64_value result;

  __x86_64_value_init(&left);
  __x86_64_value_init(&right);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&left);
  left = __x86_64_value_int(22);

  __x86_64_value_drop(&right);
  right = __x86_64_value_int(20);

  __x86_64_value_drop(&result);
  result = __x86_64_add(left, right);

  int32_t res = *result.data->raw;
  cr_expect_eq(res, 42);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&left);
  __x86_64_value_drop(&right);
}

Test(x86_64_core, sum_string) {
  x86_64_value left;
  x86_64_value right;
  x86_64_value result;

  __x86_64_value_init(&left);
  __x86_64_value_init(&right);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&left);
  left = __x86_64_value_string((const uint8_t *)"abcde");

  __x86_64_value_drop(&right);
  right = __x86_64_value_string((const uint8_t *)"fghyu");

  __x86_64_value_drop(&result);
  result = __x86_64_add(left, right);

  uint8_t *res = result.data->raw;
  debug("%zu, %s", result.data->data_size, result.data->raw);
  cr_expect_eq(result.data->data_size, strlen("abcdefghyu") + 1);
  cr_expect_str_eq((char *)res, "abcdefghyu");

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&left);
  __x86_64_value_drop(&right);
}

Test(x86_64_core, array_create) {
  x86_64_value first;
  x86_64_value second;
  x86_64_value result;

  __x86_64_value_init(&first);
  __x86_64_value_init(&second);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&first);
  first = __x86_64_value_long(10);

  __x86_64_value_drop(&second);
  second = __x86_64_value_long(8);

  __x86_64_value_drop(&result);
  result = __x86_64_add(first, second);
  debug("res: %zu, %ld", result.data->data_size, *result.data->raw);

  __x86_64_value_drop(&result);
  result = __x86_64_value_array(&(x86_64_type){.kind = X86_64_TYPE_INT}, first,
                                second, __x86_64_value_void());

  cr_expect_eq(80 * 4, result.data->data_size);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&first);
  __x86_64_value_drop(&second);
}

Test(x86_64_core, string_index) {
  x86_64_value string;
  x86_64_value index;
  x86_64_value result;

  __x86_64_value_init(&string);
  __x86_64_value_init(&index);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&string);
  string = __x86_64_value_string((const uint8_t *)"abcde");

  __x86_64_value_drop(&index);
  index = __x86_64_value_long(3);

  __x86_64_value_drop(&result);
  result = __x86_64_index_get(string, index, __x86_64_value_void());
  debug("res: %zu, %c", result.data->data_size, *result.data->raw);

  cr_expect_eq('d', (char)*result.data->raw);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&result);
  __x86_64_value_drop(&string);
  __x86_64_value_drop(&index);
}

Test(x86_64_core, array_index_1d) {
  x86_64_value array;
  x86_64_value rank_1;
  x86_64_value index;
  x86_64_value result;

  __x86_64_value_init(&array);
  __x86_64_value_init(&rank_1);
  __x86_64_value_init(&index);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&rank_1);
  rank_1 = __x86_64_value_long(10);

  __x86_64_value_drop(&index);
  index = __x86_64_value_long(1);

  __x86_64_value_drop(&array);
  array = __x86_64_value_array(&(x86_64_type){.kind = X86_64_TYPE_LONG}, rank_1,
                               __x86_64_value_void());

  result = __x86_64_index_get(array, index, __x86_64_value_void());
  debug("res: %zu, %c", result.data->data_size, *result.data->raw);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&index);
  __x86_64_value_drop(&rank_1);
  __x86_64_value_drop(&array);
}

Test(x86_64_core, string_set) {
  x86_64_value string;
  x86_64_value index;
  x86_64_value chr;

  __x86_64_value_init(&string);
  __x86_64_value_init(&index);
  __x86_64_value_init(&chr);

  __x86_64_value_drop(&string);
  string = __x86_64_value_string((const uint8_t *)"abcdef");

  __x86_64_value_drop(&index);
  index = __x86_64_value_long(1);

  __x86_64_value_drop(&chr);
  chr = __x86_64_value_char('x');

  __x86_64_index_set(chr, string, index, __x86_64_value_void());

  cr_expect_eq('x', string.data->raw[1]);

  __x86_64_value_drop(&chr);
  __x86_64_value_drop(&index);
  __x86_64_value_drop(&string);
}

Test(x86_64_core, array_set_1d) {
  x86_64_value array;
  x86_64_value rank_1;
  x86_64_value index_1;
  x86_64_value result;

  __x86_64_value_init(&array);
  __x86_64_value_init(&rank_1);
  __x86_64_value_init(&index_1);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&rank_1);
  rank_1 = __x86_64_value_long(10);

  __x86_64_value_drop(&index_1);
  index_1 = __x86_64_value_long(1);

  __x86_64_value_drop(&array);
  array = __x86_64_value_array(&(x86_64_type){.kind = X86_64_TYPE_LONG}, rank_1,
                               __x86_64_value_void());

  __x86_64_index_set(rank_1, array, index_1, __x86_64_value_void());

  __x86_64_value_drop(&result);
  result = __x86_64_index_get(array, index_1, __x86_64_value_void());

  int64_t res = *result.data->raw;
  cr_expect_eq(10, res);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&index_1);
  __x86_64_value_drop(&rank_1);
  __x86_64_value_drop(&array);
}

Test(x86_64_core, array_set_2d) {
  x86_64_value array;
  x86_64_value rank_1;
  x86_64_value index_1;
  x86_64_value index_2;
  x86_64_value result;

  __x86_64_value_init(&array);
  __x86_64_value_init(&rank_1);
  __x86_64_value_init(&index_1);
  __x86_64_value_init(&index_2);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&rank_1);
  rank_1 = __x86_64_value_long(10);

  __x86_64_value_drop(&index_1);
  index_1 = __x86_64_value_long(1);
  __x86_64_value_drop(&index_2);
  index_2 = __x86_64_value_long(2);

  __x86_64_value_drop(&array);
  array = __x86_64_value_array(&(x86_64_type){.kind = X86_64_TYPE_LONG}, rank_1,
                               rank_1, __x86_64_value_void());

  __x86_64_index_set(rank_1, array, index_1, index_2, __x86_64_value_void());

  __x86_64_value_drop(&result);
  result = __x86_64_index_get(array, index_1, index_2, __x86_64_value_void());

  int64_t res = *result.data->raw;
  cr_expect_eq(10, res);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&index_1);
  __x86_64_value_drop(&index_2);
  __x86_64_value_drop(&rank_1);
  __x86_64_value_drop(&array);
}

Test(x86_64_core, array_cast_1) {
  x86_64_value source;
  x86_64_value target;

  __x86_64_value_init(&source);
  __x86_64_value_init(&target);

  __x86_64_value_drop(&source);
  source = __x86_64_value_long(10);

  __x86_64_value_drop(&target);
  target = __x86_64_cast(source, X86_64_TYPE_BYTE);

  int64_t res = *target.data->raw;
  cr_expect_eq(10, res);

  __x86_64_value_drop(&source);
  __x86_64_value_drop(&target);
}

Test(x86_64_core, array_cast_2) {
  x86_64_value source;
  x86_64_value target;

  __x86_64_value_init(&source);
  __x86_64_value_init(&target);

  __x86_64_value_drop(&source);
  source = __x86_64_value_uint(10);

  __x86_64_value_drop(&target);
  target = __x86_64_cast(source, X86_64_TYPE_LONG);

  uint32_t res = *target.data->raw;
  cr_expect_eq(10, res);

  __x86_64_value_drop(&source);
  __x86_64_value_drop(&target);
}

Test(x86_64_core, long_shl) {
  x86_64_value first;
  x86_64_value second;
  x86_64_value result;

  __x86_64_value_init(&first);
  __x86_64_value_init(&second);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&first);
  first = __x86_64_value_long(1);

  __x86_64_value_drop(&second);
  second = __x86_64_value_long(3);

  __x86_64_value_drop(&result);
  result = __x86_64_bit_shl(first, second);

  int64_t res = *result.data->raw;
  cr_expect_eq(8, res);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&second);
  __x86_64_value_drop(&first);
}

Test(x86_64_core, bool_eq_neq) {
  x86_64_value first;
  x86_64_value second;
  x86_64_value result;
  int8_t       res;

  __x86_64_value_init(&first);
  __x86_64_value_init(&second);
  __x86_64_value_init(&result);

  __x86_64_value_drop(&first);
  first = __x86_64_value_bool(true);

  __x86_64_value_drop(&second);
  second = __x86_64_value_bool(false);

  __x86_64_value_drop(&result);
  result = __x86_64_eq(first, second);

  res = *result.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(0, res);

  __x86_64_value_drop(&result);
  result = __x86_64_neq(first, second);

  res = *result.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_neq(0, res);

  __x86_64_value_drop(&result);
  __x86_64_value_drop(&second);
  __x86_64_value_drop(&first);
}

Test(x86_64_core, bool_not) {
  x86_64_value first;
  x86_64_value second;
  int8_t       res;

  __x86_64_value_init(&first);
  __x86_64_value_init(&second);

  __x86_64_value_drop(&first);
  first = __x86_64_value_bool(true);

  res = *first.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(1, res);

  __x86_64_value_drop(&second);
  second = __x86_64_not(first);

  res = *second.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(0, res);

  __x86_64_value_drop(&first);
  first = __x86_64_not(second);

  res = *first.data->raw;
  res = res > 0 ? 1 : 0;
  cr_expect_eq(1, res);

  __x86_64_value_drop(&second);
  __x86_64_value_drop(&first);
}

Test(x86_64_core, assign) {
  x86_64_value lsv;
  x86_64_value rsv;

  __x86_64_value_init(&lsv);
  __x86_64_value_init(&rsv);

  __x86_64_value_drop(&rsv);
  rsv = __x86_64_value_string((uint8_t *)"something");

  __x86_64_assign(&lsv, rsv);

  cr_expect_str_eq("something", (char *)rsv.data->raw);
  cr_expect_str_eq((char *)rsv.data->raw, (char *)lsv.data->raw);
  __x86_64_value_drop(&rsv);

  cr_expect_str_eq("something", (char *)lsv.data->raw);
  __x86_64_value_drop(&lsv);

  rsv = __x86_64_value_string((uint8_t *)"something");
  __x86_64_assign(&rsv, rsv);

  cr_expect_str_eq("something", (char *)rsv.data->raw);
  __x86_64_value_drop(&rsv);
}

Test(x86_64_core, cond) {
  x86_64_value rsv;

  __x86_64_value_init(&rsv);

  __x86_64_value_drop(&rsv);
  rsv = __x86_64_value_bool(true);

  uint8_t v = __x86_64_unwrap_bool(rsv);
  cr_expect_neq(0, v);

  __x86_64_value_drop(&rsv);
}

Test(x86_64_core, cond2) {
  x86_64_value rsv;

  __x86_64_value_init(&rsv);

  __x86_64_value_drop(&rsv);
  rsv = __x86_64_value_bool(false);

  uint8_t v = __x86_64_unwrap_bool(rsv);
  cr_expect_eq(0, v);

  __x86_64_value_drop(&rsv);
}
