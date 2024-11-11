#include "type.h"

#include "util/log.h"
#include "util/macro.h"

#include <assert.h>
#include <stdlib.h>
#include <string.h>

static void type_base_deinit(type_base *self) { span_free(self->span); }

static void type_base_free(type_base *self) {
  if (self) {
    type_base_deinit(self);
    free(self);
  }
}

static void type_base_init(type_base *self, type_enum type, span *span) {
  self->type = type;
  self->span = span;
}

static type_list *type_list_copy(type_list *list) {
  type_list *new_list = type_list_new();
  for (list_node *it = list->list.head; it; it = it->next) {
    type_base *generic = it->data;
    type_list_push_back(new_list, type_copy(generic));
  }
  return new_list;
}

type_base *type_base_new(type_enum type, span *span) {
  type_base *self = MALLOC(type_base);
  type_base_init(self, type, span);
  return self;
}

// ARRAY
type_array *type_array_new(span *span, size_t dimensions,
                           type_base *elem_type) {
  type_array *self = MALLOC(type_array);
  type_base_init(&self->base, TYPE_ARRAY, span);
  self->dimensions = dimensions;
  self->elem_type = elem_type;
  return self;
}

static void type_array_free(type_array *self) {
  if (self) {
    type_base_deinit(&self->base);
    type_free(self->elem_type);
    free(self);
  }
}

// CUSTOM
type_custom *type_custom_new(span *span, const char *name) {
  type_custom *self = MALLOC(type_custom);
  type_base_init(&self->base, TYPE_CUSTOM, span);
  self->name = strdup(name);
  return self;
}

static void type_custom_free(type_custom *self) {
  if (self) {
    type_base_free(&self->base);
    free(self->name);
    free(self);
  }
}
// CALLABLE
type_callable *type_callable_new(span *span, type_base *ret_type,
                                 type_list *param_types) {
  type_callable *self = MALLOC(type_callable);
  type_base_init(&self->base, TYPE_CALLABLE, span);
  self->ret_type = ret_type;
  self->param_types = param_types;
  return self;
}

static void type_callable_free(type_callable *self) {
  if (self) {
    type_free(self->ret_type);
    type_list_free(self->param_types);
    free(self);
  }
}

// GENERIC
void type_free(type_base *generic) {
  if (generic) {
    switch (generic->type) {
    case TYPE_ARRAY:
      return type_array_free((type_array *)generic);
    case TYPE_CUSTOM:
      return type_custom_free((type_custom *)generic);
    case TYPE_CALLABLE:
      return type_callable_free((type_callable *)generic);
    default:
      return type_base_free(generic);
    }
  }
}

// if both types are not null and different -> not equal
int type_equals(const type_base *lsv, const type_base *rsv) {
  if (!lsv || !rsv) {
    return 1;
  }
  if (lsv->type != rsv->type) {
    return 0;
  }
  switch (lsv->type) {
  case TYPE_ARRAY: {
    type_array *lsv_arr = (type_array *)lsv;
    type_array *rsv_arr = (type_array *)rsv;
    if (lsv_arr->dimensions != rsv_arr->dimensions) {
      return 0;
    }
    return type_equals(lsv_arr->elem_type, rsv_arr->elem_type);
  }
  case TYPE_CUSTOM: {
    type_custom *lsv_cus = (type_custom *)lsv;
    type_custom *rsv_cus = (type_custom *)rsv;
    return !strcmp(lsv_cus->name, rsv_cus->name);
  }
  case TYPE_CALLABLE: {
    type_callable *lsv_cal = (type_callable *)lsv;
    type_callable *rsv_cal = (type_callable *)rsv;
    int            ret = type_equals(lsv_cal->ret_type, rsv_cal->ret_type);
    if (ret) {
      return ret;
    }
    size_t lsize = type_list_size(lsv_cal->param_types);
    size_t rsize = type_list_size(rsv_cal->param_types);
    if (lsize != rsize) {
      return 1;
    }
    for (list_node *lit = lsv_cal->param_types->list.head,
                   *rit = rsv_cal->param_types->list.head;
         lit; lit = lit->next, rit = rit->next) {
      ret = type_equals(lit->data, rit->data);
      if (ret) {
        return ret;
      }
    }
    return 0;
  }
  default:
    return 1;
  }
}

type_base *type_copy(const type_base *self) {
  if (!self) {
    return NULL;
  }
  switch (self->type) {
  case TYPE_ARRAY: {
    type_array *array = (type_array *)self;
    return (type_base *)type_array_new(span_copy(self->span), array->dimensions,
                                       type_copy(array->elem_type));
  }
  case TYPE_CUSTOM: {
    type_custom *custom = (type_custom *)self;
    return (type_base *)type_custom_new(span_copy(self->span), custom->name);
  }
  case TYPE_CALLABLE: {
    type_callable *callable = (type_callable *)self;
    return (type_base *)type_callable_new(
        span_copy(self->span), type_copy(callable->ret_type),
        type_list_copy(callable->param_types));
  }
  default:
    return type_base_new(self->type, span_copy(self->span));
  }
}

const char *type_base_str(type_base *self) {
  if (self) {
    return type_enum_str(self->type);
  }
  return "<null>";
}

const char *type_enum_str(type_enum type) {
  switch (type) {
  case TYPE_BOOL:
    return "BOOL";
  case TYPE_BYTE:
    return "BYTE";
  case TYPE_INT:
    return "INT";
  case TYPE_UINT:
    return "UINT";
  case TYPE_LONG:
    return "LONG";
  case TYPE_ULONG:
    return "ULONG";
  case TYPE_CHAR:
    return "CHAR";
  case TYPE_STRING:
    return "STRING";
  case TYPE_VOID:
    return "VOID";
  case TYPE_ARRAY:
    return "ARRAY";
  case TYPE_CUSTOM:
    return "CUSTOM";
  case TYPE_CALLABLE:
    return "CALLABLE";
  case TYPE_UNKNOWN:
  default:
    return "UNKNOWN";
  }
}
