#pragma once

#include <stdio.h>

struct dot_string_struct {
  const char *chars;
  const size_t len;
};

typedef struct dot_string_struct dot_string;
