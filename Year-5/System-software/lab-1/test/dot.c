#include <criterion/criterion.h>

#include "dot.h"

Test(dot, simple) {
  dot_string str = dot_ast(NULL);

  printf("%s", str.chars);
  printf("\n---------------\n");
  printf("len: %lu", str.len);

  free((void *)str.chars);
}
