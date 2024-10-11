#include <criterion/criterion.h>

#include "dot.h"

Test(dot, simple) {
  dot_string str = dot_ast(NULL);

  free((void *)str.chars);
}
