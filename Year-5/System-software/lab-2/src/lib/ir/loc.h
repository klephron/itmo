#pragma once

#include <antlr3interfaces.h>
#include <stddef.h>

struct ir_loc {
  int    complete;
  size_t line;
  size_t pos_start;
  size_t pos_end;
};

typedef struct ir_loc ir_loc;

ir_loc ir_loc_antlr3(pANTLR3_BASE_TREE tree);
