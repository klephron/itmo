#include "option2_inter.h"
#include "util/macro.h"

#include <stdlib.h>

void option2_inter_free(option2_inter *inter) {
  if (inter != NULL) {
    inter->subroutines->free(inter->subroutines);
    inter->exceptions->free(inter->exceptions);
  }
}

static struct option2_inter *option2_inter_new() {
  option2_inter *inter = MALLOC(option2_inter);
  inter->subroutines = NULL; // TODO: list_new
  inter->exceptions = NULL;  // TODO: list_new
  inter->free = option2_inter_free;
  return inter;
}

option2_inter *option2_inter_build(const p_list ast) {
  option2_inter *inter = option2_inter_new();

  return inter;
}
