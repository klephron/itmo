#include <criterion/criterion.h>

#include "util/list.h"

Test(simple, test) {
  p_list list = list_new();
  list->put(list, "first", 5); 
  cr_expect_eq(5, list->get(list, "first").value);
  list->free(list);
}
