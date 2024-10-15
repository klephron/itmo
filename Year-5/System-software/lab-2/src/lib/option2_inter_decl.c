#include "option2_inter_decl.h"

#include "exception.h"
#include "util/list_funcs.h"
#include "util/macro.h"
#include <stdlib.h>

static void inter_t_free(struct option2_inter_t_base *generic);

static void inter_t_base_free_in(struct option2_inter_t_base *base) {
  UNUSED(base);
}

static void inter_t_base_free(struct option2_inter_t_base *base) {
  if (base != NULL) {
    inter_t_base_free_in(base);
    free(base);
  }
}

static void inter_t_array_free(struct option2_inter_t_base *base) {
  struct option2_inter_t_array *array = (struct option2_inter_t_array *)base;
  if (array != NULL) {
    inter_t_base_free_in(&array->base);
    inter_t_free(array->elem_type);
    free(array);
  }
}

static void inter_t_free(struct option2_inter_t_base *generic) {
  if (generic != NULL) {
    switch (generic->type) {
    case OPTION2_INTER_T_ARRAY:
      inter_t_array_free(generic);
    default:
      inter_t_base_free(generic);
    }
  }
}

static void signature_free(option2_inter_signature *data) {
  free(data->name);
  data->params->free(data->params);
  inter_t_free(data->ret);
}

static void subroutine_free(option2_inter_subroutine *data) {
  if (data != NULL) {
    signature_free(data->signature);
    free(data->file_name);
    // TODO: free blocks (tree-like structure)
    data->operations->free(data->operations);
  }
}

static void inter_free(option2_inter *inter) {
  if (inter != NULL) {
    inter->subroutines->free(inter->subroutines);
    inter->exceptions->free(inter->exceptions);
    free(inter);
  }
}

// move
static void *sub_list_new(void *_data) {
  option2_inter_subroutine *exc = _data;
  return exc;
}

static void sub_list_delete(void *_data) {
  if (_data != NULL) {
    option2_inter_subroutine *exc = _data;
    subroutine_free(exc);
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

static void option2_inter_t_base_init(option2_inter_t_base *self,
                                      option2_inter_t_enum type) {
  self->type = type;
}

option2_inter_t_base *option2_inter_t_base_new(option2_inter_t_enum type) {
  option2_inter_t_base *self = MALLOC(option2_inter_t_base);
  option2_inter_t_base_init(self, type);
  return self;
}

option2_inter_t_array *
option2_inter_t_array_new(size_t dimensions, option2_inter_t_base *elem_type) {
  option2_inter_t_array *self = MALLOC(option2_inter_t_array);
  option2_inter_t_base_init(&self->base, OPTION2_INTER_T_ARRAY);
  self->dimensions = dimensions;
  self->elem_type = elem_type;
  return self;
}

option2_inter_signature *
option2_inter_signature_new(char *name, p_list params,
                            option2_inter_t_base *ret) {
  option2_inter_signature *self = MALLOC(option2_inter_signature);
  self->name = name;
  self->params = params;
  self->ret = ret;
  return self;
}

option2_inter_subroutine *
option2_inter_subroutine_new(const option2_inter_userp *userp,
                             option2_inter_signature *signature, void *blocks,
                             p_list operations) {
  option2_inter_subroutine *self = MALLOC(option2_inter_subroutine);
  self->signature = signature;
  self->file_name = (char *)userp->file_name;
  self->blocks = blocks;
  self->operations = operations;
  return self;
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
