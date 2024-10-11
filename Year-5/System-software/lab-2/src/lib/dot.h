#pragma once

#include <antlr3interfaces.h>
#include <stdio.h>

struct dot_string_struct {
  const char *chars;
  const size_t len;
};

typedef struct dot_string_struct dot_string;

dot_string dot_ast(pANTLR3_BASE_TREE tree);
