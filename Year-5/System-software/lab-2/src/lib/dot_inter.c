#include "dot_inter.h"
#include <stdlib.h>

// TODO: implement
dot_string *dot_inter_cfg(const option2_inter *inter,
                          const option2_inter_subroutine *sub) {
  size_t len = 10;
  char *chars = malloc(len);

  return dot_string_new(chars, len);
}

// TODO: implement
dot_string *dot_inter_cg(const option2_inter *inter,
                         const option2_inter_subroutine *sub) {
  size_t len = 10;
  char *chars = malloc(len);

  return dot_string_new(chars, len);
}
