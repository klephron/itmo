#include "option2_inter/types.h"

#include "util/macro.h"
#include <stdlib.h>


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
    option2_inter_t_free(array->elem_type);
    free(array);
  }
}

void option2_inter_t_free(struct option2_inter_t_base *generic) {
  if (generic != NULL) {
    switch (generic->type) {
    case OPTION2_INTER_T_ARRAY:
      inter_t_array_free(generic);
    default:
      inter_t_base_free(generic);
    }
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
