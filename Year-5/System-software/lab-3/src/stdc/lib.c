#include "lib.h"

#include <stdio.h>

int32_t read_int() {
  int32_t v;
  scanf("%d", &v);
  return v;
}

int64_t read_long() {
  int64_t v;
  scanf("%ld", &v);
  return v;
}
