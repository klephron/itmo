#include "ir/types.h"

#include "util/log.h"
#include "util/macro.h"

#include <assert.h>
#include <stdlib.h>
#include <string.h>

static void ir_t_base_free_in(ir_t_base *base) { UNUSED(base); }

static void ir_t_base_free(ir_t_base *base) {
  if (base != NULL) {
    ir_t_base_free_in(base);
    free(base);
  }
}

static void ir_t_array_free(ir_t_base *base) {
  struct ir_t_array *array = (struct ir_t_array *)base;
  if (array != NULL) {
    ir_t_base_free_in(&array->base);
    ir_t_free(array->elem_type);
    free(array);
  }
}

void ir_t_free(ir_t_base *generic) {
  if (generic != NULL) {
    switch (generic->type) {
    case IR_T_ARRAY:
      ir_t_array_free(generic);
    default:
      ir_t_base_free(generic);
    }
  }
}

static void ir_t_base_init(ir_t_base *self, ir_t_enum type) {
  self->type = type;
}

static char *ttstr(ir_t_enum type) {
  switch (type) {
  case IR_T_BOOL:
    return "BOOL";
  case IR_T_BYTE:
    return "BYTE";
  case IR_T_INT:
    return "INT";
  case IR_T_UINT:
    return "UINT";
  case IR_T_LONG:
    return "LONG";
  case IR_T_ULONG:
    return "ULONG";
  case IR_T_CHAR:
    return "CHAR";
  case IR_T_STRING:
    return "STRING";
  case IR_T_VOID:
    return "VOID";
  case IR_T_ARRAY:
    return "ARRAY";
  case IR_T_CUSTOM:
    return "CUSTOM";
  case IR_T_UNKNOWN:
  default:
    return "UNKNOWN";
  }
}

ir_t_base *ir_t_base_new(ir_t_enum type) {
  ir_t_base *self = MALLOC(ir_t_base);
  ir_t_base_init(self, type);
  debug("created base type=%s", ttstr(self->type));
  return self;
}

ir_t_array *ir_t_array_new(size_t dimensions, ir_t_base *elem_type) {
  ir_t_array *self = MALLOC(ir_t_array);
  ir_t_base_init(&self->base, IR_T_ARRAY);
  self->dimensions = dimensions;
  self->elem_type = elem_type;
  debug("created array elem_type=%s,dimensions=%d",
        ttstr(self->elem_type->type), self->dimensions);
  return self;
}

ir_t_custom *ir_t_custom_new(char *name) {
  ir_t_custom *self = MALLOC(ir_t_custom);
  ir_t_base_init(&self->base, IR_T_CUSTOM);
  self->name = name;
  debug("created custom name=%s", name);
  return self;
}

int ir_t_equals(ir_t_base *lsv, ir_t_base *rsv) {
  if (lsv == NULL || rsv == NULL) {
    return 0;
  }
  if (lsv->type != rsv->type) {
    return 0;
  }
  if (lsv->type == IR_T_ARRAY) {
    ir_t_array *lsv_arr = (ir_t_array *)lsv;
    ir_t_array *rsv_arr = (ir_t_array *)rsv;
    if (lsv_arr->dimensions != rsv_arr->dimensions) {
      return 0;
    }
    return ir_t_equals(lsv_arr->elem_type, rsv_arr->elem_type);
  } else if (lsv->type == IR_T_CUSTOM) {
    ir_t_custom *lsv_cus = (ir_t_custom *)lsv;
    ir_t_custom *rsv_cus = (ir_t_custom *)rsv;
    return !strcmp(lsv_cus->name, rsv_cus->name);
  }
  return 1;
}
