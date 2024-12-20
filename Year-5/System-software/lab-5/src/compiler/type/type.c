#include "type.h"

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

static list_type *type_list_copy(list_type *list) {
  list_type *new_list = list_type_new();
  for (list_node *it = list->list.head; it; it = it->next) {
    type_base *generic = it->data;
    list_type_push_back(new_list, type_copy(generic));
  }
  return new_list;
}

type_base *type_base_new(span *span, type_enum type) {
  type_base *self = MALLOC(type_base);
  type_base_init(self, type, span);
  return self;
}

// ARRAY
type_array *type_array_new(span *span, type_base *elem_type) {
  type_array *self = MALLOC(type_array);
  type_base_init(&self->base, TYPE_ARRAY, span);
  self->elem_type = elem_type;
  return self;
}

static void type_array_free(type_array *self) {
  if (self) {
    type_free(self->elem_type);
    type_base_deinit(&self->base);
    free(self);
  }
}

// CUSTOM
type_custom *type_custom_new(span *span, char *name) {
  type_custom *self = MALLOC(type_custom);
  type_base_init(&self->base, TYPE_CUSTOM, span);
  self->name = name;
  return self;
}

static void type_custom_free(type_custom *self) {
  if (self) {
    if (self->name) {
      free(self->name);
    }
    type_base_deinit(&self->base);
    free(self);
  }
}
// CALLABLE
type_callable *type_callable_new(span *span, type_base *ret_type,
                                 list_type *param_types) {
  type_callable *self = MALLOC(type_callable);
  type_base_init(&self->base, TYPE_CALLABLE, span);
  self->ret_type = ret_type;
  self->param_types = param_types;
  return self;
}

static void type_callable_free(type_callable *self) {
  if (self) {
    type_free(self->ret_type);
    list_type_free(self->param_types);
    type_base_deinit(&self->base);
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
    size_t lsize = list_type_size(lsv_cal->param_types);
    size_t rsize = list_type_size(rsv_cal->param_types);
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
    return (type_base *)type_array_new(span_copy(self->span),
                                       type_copy(array->elem_type));
  }
  case TYPE_CUSTOM: {
    type_custom *custom = (type_custom *)self;
    return (type_base *)type_custom_new(span_copy(self->span),
                                        strdup(custom->name));
  }
  case TYPE_CALLABLE: {
    type_callable *callable = (type_callable *)self;
    return (type_base *)type_callable_new(
        span_copy(self->span), type_copy(callable->ret_type),
        type_list_copy(callable->param_types));
  }
  default:
    return type_base_new(span_copy(self->span), self->type);
  }
}

list_type *list_type_copy(const list_type *in) {
  if (!in) {
    return NULL;
  }
  list_type *nlist = list_type_new();
  for (list_type_it it = list_type_begin((list_type *)in); !it.end(&it);
       it.next(&it)) {
    list_type_push_back(nlist, type_copy(it.get(&it)));
  }
  return nlist;
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
