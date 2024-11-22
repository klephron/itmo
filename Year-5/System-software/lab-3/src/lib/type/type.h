#pragma once

#include "span/span.h"
#include "util/container_util.h"
#include "util/list.h"
#include <stddef.h>

typedef enum type_enum {
  TYPE_UNKNOWN,
  TYPE_BOOL,
  TYPE_BYTE,
  TYPE_INT,
  TYPE_UINT,
  TYPE_LONG,
  TYPE_ULONG,
  TYPE_CHAR,
  TYPE_STRING,
  TYPE_VOID,
  TYPE_ARRAY,
  TYPE_CUSTOM,
  TYPE_CALLABLE,
} type_enum;

typedef struct type_base_struct {
  type_enum type;
  span     *span;
} type_base;

void       type_free(type_base *generic);
int        type_equals(const type_base *lsv, const type_base *rsv);
type_base *type_copy(const type_base *generic);

static inline int container_cmp_type(const void *lsv, const void *rsv) {
  return !type_equals(lsv, rsv);
}
static inline void container_delete_type(void *self) { return type_free(self); }
LIST_DECLARE_STATIC_INLINE(list_type, type_base, container_cmp_type,
                           container_new_move, container_delete_type);

list_type *list_type_copy(const list_type *in);

typedef struct type_array_struct {
  type_base  base;
  type_base *elem_type;
} type_array;

typedef struct type_custom_struct {
  type_base base;
  char     *name;
} type_custom;

typedef struct type_callable_struct {
  type_base  base;
  type_base *ret_type;
  list_type *param_types;
} type_callable;

const char *type_enum_str(type_enum type);

type_base  *type_base_new(span *span, type_enum type);
const char *type_base_str(type_base *type);

type_array    *type_array_new(span *span, type_base *elem_type);
type_custom   *type_custom_new(span *span, char *name);
type_callable *type_callable_new(span *span, type_base *ret_type,
                                 list_type *param_types);
