#include <criterion/criterion.h>
#include <stdio.h>

#include "util/list.h"
#include "util/list_funcs.h"

Test(list, create_delete) {
  p_list list =
      list_new(string_equals, string_string_new, string_string_delete, 1);

  cr_expect_eq(list->equals, string_equals);
  cr_expect_eq(list->new, string_string_new);
  cr_expect_eq(list->delete, string_string_delete);

  list->free(list);
}

Test(list, put) {
  p_list list =
      list_new(string_equals, string_string_new, string_string_delete, 1);

  list->put(list, "hello", "world");

  list->free(list);
}

Test(list, put_put) {
  p_list list =
      list_new(string_equals, string_string_new, string_string_delete, 1);

  list->put(list, "hello", "world");
  list->put(list, "world", "world2");

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list->free(list);
}

Test(list, put_putsame_unique) {
  p_list list =
      list_new(string_equals, string_string_new, string_string_delete, 1);

  list->put(list, "hello", "world");
  list->put(list, "hello", "world2");

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1, cnt);

  list->free(list);
}

Test(list, put_putsame) {
  p_list list =
      list_new(string_equals, string_string_new, string_string_delete, 0);

  list->put(list, "hello", "world");
  list->put(list, "hello", "world2");

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list->free(list);
}

Test(list, put_many) {
  p_list list =
      list_new(string_equals, string_string_new, string_string_delete, 1);

  char key[10];
  char value[10];

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list->put(list, key, value);
  }

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1000, cnt);

  list->free(list);
}

Test(list, put_many_check) {
  p_list list =
      list_new(string_equals, string_string_new, string_string_delete, 1);

  char key[16];
  char value[16];

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list->put(list, key, value);
  }

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt) {
    sprintf(key, "%d", cnt);
    sprintf(value, "%d", cnt + 1000);

    cr_expect_str_eq(key, it->data->key);
    cr_expect_str_eq(value, it->data->value);
  }

  cr_expect_eq(1000, cnt);

  list->free(list);
}
