#pragma once

#include <stdio.h>

struct dot_string_struct {
  char *chars;
  size_t len;

  void (*free)(struct dot_string_struct *);
};

struct dot_string_struct *dot_string_new(char *chars, size_t len);

typedef struct dot_string_struct dot_string;
