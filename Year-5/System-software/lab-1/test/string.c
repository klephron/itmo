#include <criterion/criterion.h>

#include "util/string.h"

Test(escape_html, simple) {
  const char *str = "asd[sd";

  const char *out = escape_html(str);

  cr_assert_str_eq("asd&#91;sd", out);

  free((void *)out);
}
