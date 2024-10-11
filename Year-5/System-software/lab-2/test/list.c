#include <criterion/criterion.h>
#include <stdio.h>

#include "util/list.h"
#include "util/list_funcs.h"

Test(list, create_delete) {
  p_list list = list_new(list_string_string_cmp, list_string_string_new,
                         list_string_string_delete, 1);

  cr_expect_eq(list->f_cmp, list_string_string_cmp);
  cr_expect_eq(list->f_new, list_string_string_new);
  cr_expect_eq(list->f_delete, list_string_string_delete);

  list->free(list);
}

Test(list, put) {
  p_list list = list_new(list_string_string_cmp, list_string_string_new,
                         list_string_string_delete, 1);

  string_string data = {.key = "hello", .value = "world"};

  list->put(list, &data);
  list->free(list);
}

Test(list, put_put) {
  p_list list = list_new(list_string_string_cmp, list_string_string_new,
                         list_string_string_delete, 1);

  string_string data = {.key = "hello", .value = "world"};
  list->put(list, &data);
  data.key = "hello2";
  data.value = "world2";
  list->put(list, &data);

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list->free(list);
}

Test(list, put_putsame_unique) {
  p_list list = list_new(list_string_string_cmp, list_string_string_new,
                         list_string_string_delete, 1);

  string_string data = {.key = "hello", .value = "world"};
  list->put(list, &data);
  data.value = "world2";
  list->put(list, &data);

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1, cnt);

  list->free(list);
}

Test(list, put_putsame) {
  p_list list = list_new(list_string_string_cmp, list_string_string_new,
                         list_string_string_delete, 0);

  string_string data = {.key = "hello", .value = "world"};
  list->put(list, &data);
  data.value = "world2";
  list->put(list, &data);

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list->free(list);
}

Test(list, put_many) {
  p_list list = list_new(list_string_string_cmp, list_string_string_new,
                         list_string_string_delete, 1);

  char key[10];
  char value[10];

  string_string data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list->put(list, &data);
  }

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1000, cnt);

  list->free(list);
}

Test(list, put_many_check) {
  p_list list = list_new(list_string_string_cmp, list_string_string_new,
                         list_string_string_delete, 1);

  char key[16];
  char value[16];

  string_string data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list->put(list, &data);
  }

  int cnt = 0;
  for (list_iter it = list->head; it != NULL; it = it->next, ++cnt) {
    string_string *data = it->data;
    sprintf(key, "%d", cnt);
    sprintf(value, "%d", cnt + 1000);

    cr_expect_str_eq(key, data->key);
    cr_expect_str_eq(value, data->value);
  }

  cr_expect_eq(1000, cnt);

  list->free(list);
}
