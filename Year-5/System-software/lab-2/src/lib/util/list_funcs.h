#pragma once

#include "util/list.h"

struct string_string {
  char *key;
  char *value;
};

typedef struct string_string string_string;

list_func_equals list_string_string_equals;
list_func_new list_string_string_new;
list_func_delete list_string_string_delete;

list_func_equals list_false_equals;
