#include <criterion/criterion.h>
#include <stdio.h>

#include "util/container_funcs.h"
#include "util/list.h"

Test(list, create_delete) {
  p_list list =
      list_new(container_cmp_string_string, container_new_string_string,
               container_delete_string_string);

  cr_expect_eq(list->f_cmp, container_cmp_string_string);
  cr_expect_eq(list->f_new, container_new_string_string);
  cr_expect_eq(list->f_delete, container_delete_string_string);

  list->free(list);
}

Test(list, push_back) {
  p_list list =
      list_new(container_cmp_string_string, container_new_string_string,
               container_delete_string_string);

  string_string data = {.key = "hello", .value = "world"};

  list->push_back(list, &data);
  list->free(list);
}

Test(list, push_back2) {
  p_list list =
      list_new(container_cmp_string_string, container_new_string_string,
               container_delete_string_string);

  string_string data = {.key = "hello", .value = "world"};
  list->push_back(list, &data);
  data.key = "hello2";
  data.value = "world2";
  list->push_back(list, &data);

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list->free(list);
}

Test(list, find_insert) {
  p_list list =
      list_new(container_cmp_string_string, container_new_string_string,
               container_delete_string_string);

  string_string data = {.key = "hello", .value = "world"};
  list->push_back(list, &data);
  data.value = "world2";
  list->insert(list, list->find(list, &data), &data);

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1, cnt);

  list->free(list);
}

Test(list, push_push) {
  p_list list =
      list_new(container_cmp_string_string, container_new_string_string,
               container_delete_string_string);

  string_string data = {.key = "hello", .value = "world"};
  list->push_back(list, &data);
  data.value = "world2";
  list->push_back(list, &data);

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(2, cnt);

  list->free(list);
}

Test(list, put_many) {
  p_list list =
      list_new(container_cmp_string_string, container_new_string_string,
               container_delete_string_string);

  char key[10];
  char value[10];

  string_string data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list->push_back(list, &data);
  }

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt)
    ;

  cr_expect_eq(1000, cnt);

  list->free(list);
}

Test(list, put_many_check) {
  p_list list =
      list_new(container_cmp_string_string, container_new_string_string,
               container_delete_string_string);

  char key[16];
  char value[16];

  string_string data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    list->push_back(list, &data);
  }

  int cnt = 0;
  for (list_it it = list->head; it != NULL; it = it->next, ++cnt) {
    string_string *data = it->data;
    sprintf(key, "%d", cnt);
    sprintf(value, "%d", cnt + 1000);

    cr_expect_str_eq(key, data->key);
    cr_expect_str_eq(value, data->value);
  }

  cr_expect_eq(1000, cnt);

  list->free(list);
}
