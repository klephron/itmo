#include "option2_inter/ctx.h"

#include "exception.h"
#include "option2_inter/functions.h"
#include "util/list_funcs.h"
#include "util/macro.h"
#include <stdlib.h>

// move
static void *sub_list_new(void *_data) {
  option2_inter_subroutine *exc = _data;
  return exc;
}

static void sub_list_delete(void *_data) {
  if (_data != NULL) {
    option2_inter_subroutine *exc = _data;
    option2_inter_subroutine_free(exc);
  }
}

// move
static void *exc_list_new(void *_data) {
  exception *exc = _data;
  return exc;
}

static void exc_list_delete(void *_data) {
  if (_data != NULL) {
    exception *exc = _data;
    exc->free(exc);
  }
}

static void inter_free(option2_inter *inter) {
  if (inter != NULL) {
    inter->subroutines->free(inter->subroutines);
    inter->exceptions->free(inter->exceptions);
    free(inter);
  }
}

option2_inter *option2_inter_new() {
  option2_inter *inter = MALLOC(option2_inter);
  inter->subroutines =
      list_new(list_false_cmp, sub_list_new, sub_list_delete, 0);
  inter->exceptions =
      list_new(list_false_cmp, exc_list_new, exc_list_delete, 0);
  inter->free = inter_free;
  return inter;
}
