#include "dot_ir.h"

#include <stdlib.h>

// TODO: implement
dot_string *dot_inter_cfg(const ir_ctx *ctx, const ir_subroutine *sub) {
  size_t len = 10;
  char *chars = malloc(len);

  return dot_string_new(chars, len);
}

// TODO: implement
dot_string *dot_inter_cg(const ir_ctx *ctx, const ir_subroutine *sub) {
  size_t len = 10;
  char *chars = malloc(len);

  return dot_string_new(chars, len);
}
