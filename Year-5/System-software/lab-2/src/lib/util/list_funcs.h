#pragma once

#include "util/list.h"

struct string_string {
  char *key;
  char *value;
};

typedef struct string_string string_string;

list_func_cmp list_string_string_cmp;
list_func_new list_string_string_new;
list_func_delete list_string_string_delete;

list_func_cmp list_chars_cmp;
list_func_new list_chars_new;
list_func_delete list_chars_delete;

list_func_cmp list_false_cmp;
