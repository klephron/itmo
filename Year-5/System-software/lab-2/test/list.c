#include <criterion/criterion.h>
#include <stdio.h>

#include "util/container_util.h"
#include "util/list.h"

Test(list, create_delete) {
  p_list list = list_new(container_cmp_chars_chars, container_new_chars_chars,
                         container_delete_chars_chars);

  cr_expect_eq(list->f_cmp, container_cmp_chars_chars);
  cr_expect_eq(list->f_new, container_new_chars_chars);
  cr_expect_eq(list->f_delete, container_delete_chars_chars);

  list_free(list);
}

Test(list, push_back) {
  p_list list = list_new(container_cmp_chars_chars, container_new_chars_chars,
                         container_delete_chars_chars);

  chars_chars data = {.key = "hello", .value = "world"};

  list_push_back(list, &data);
  list_free(list);
}

Test(list, push_back2) {
  p_list list = list_new(container_cmp_chars_chars, container_new_chars_chars,
                         container_delete_chars_chars);

  chars_chars data = {.key = "hello", .value = "world"};
  list_push_back(list, &data);
  data.key = "hello2";
  data.value = "world2";
  list_push_back(list, &data);

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list_free(list);
}

Test(list, find_insert) {
  p_list list = list_new(container_cmp_chars_chars, container_new_chars_chars,
                         container_delete_chars_chars);

  chars_chars data = {.key = "hello", .value = "world"};
  list_push_back(list, &data);
  data.value = "world2";
  list_insert(list, list_find(list, &data), &data);

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1, cnt);

  list_free(list);
}

Test(list, push_push) {
  p_list list = list_new(container_cmp_chars_chars, container_new_chars_chars,
                         container_delete_chars_chars);

  chars_chars data = {.key = "hello", .value = "world"};
  list_push_back(list, &data);
  data.value = "world2";
  list_push_back(list, &data);

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list_free(list);
}

Test(list, put_many) {
  p_list list = list_new(container_cmp_chars_chars, container_new_chars_chars,
                         container_delete_chars_chars);

  char key[10];
  char value[10];

  chars_chars data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list_push_back(list, &data);
  }

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1000, cnt);

  list_free(list);
}

Test(list, put_many_check) {
  p_list list = list_new(container_cmp_chars_chars, container_new_chars_chars,
                         container_delete_chars_chars);

  char key[16];
  char value[16];

  chars_chars data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list_push_back(list, &data);
  }

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt) {
    chars_chars *data = it->data;
    sprintf(key, "%d", cnt);
    sprintf(value, "%d", cnt + 1000);

    cr_expect_str_eq(key, data->key);
    cr_expect_str_eq(value, data->value);
  }

  cr_expect_eq(1000, cnt);

  list_free(list);
}
