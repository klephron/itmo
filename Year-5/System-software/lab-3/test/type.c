#include <criterion/criterion.h>

#include "compiler/type/str.h"
#include "util/log.h"
#include <stdio.h>

Test(type, test) {
  type_base *root;

  type_base *type = (type_base *)type_callable_new(
      NULL,
      (type_base *)type_array_new(
          NULL, (type_base *)type_custom_new(NULL, strdup("custom_1"))),
      list_type_new());

  list_type *type_params = list_type_new();
  list_type_push_back(type_params, type_copy(type));
  list_type_push_back(type_params, type);

  root = (type_base *)type_callable_new(
      NULL,
      (type_base *)type_callable_new(
          NULL,
          (type_base *)type_callable_new(NULL, type_base_new(NULL, TYPE_STRING),
                                         type_params),
          list_type_new()),
      list_type_new());

  char *str = type_str(root);
  debug("%s", str);
  free(str);
  type_free(root);
}
