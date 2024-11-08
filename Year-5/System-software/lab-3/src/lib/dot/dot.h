#pragma once

#include <stdio.h>

struct dot_string {
  char  *chars;
  size_t len;
};

typedef struct dot_string dot_string;

struct dot_string *dot_string_new(char *chars, size_t len);
void               dot_string_free(dot_string *);
