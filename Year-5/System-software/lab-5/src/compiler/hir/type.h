#pragma once

#include "compiler/span/span.h"
#include "util/container_util.h"
#include "util/list.h"
#include <stddef.h>

// type - definition of type (not declaration, so objects won't have fields
// inside type). Type declarations are inside HIR (like for custom types there
// are variables, methods).

typedef enum hir_type_enum {
  HIR_TYPE_UNKNOWN,
  // primitives
  HIR_TYPE_BOOL,
  HIR_TYPE_BYTE,
  HIR_TYPE_INT,
  HIR_TYPE_UINT,
  HIR_TYPE_LONG,
  HIR_TYPE_ULONG,
  HIR_TYPE_CHAR,
  HIR_TYPE_STRING,
  HIR_TYPE_VOID,
  // when initially building templates/classes can't be resolved without scopes
  HIR_TYPE_CUSTOM,
  // non-primitive (can have templates)
  HIR_TYPE_ARRAY,
  HIR_TYPE_CLASS,
} hir_type_enum;

// BASE
typedef struct hir_type_base_struct {
  hir_type_enum type;
  span         *span;
} hir_type_base;

hir_type_base *hir_type_base_new(span *span, hir_type_enum type);

// GENERIC
void hir_type_free(hir_type_base *generic);
int  hir_type_equals(const hir_type_base *lsv, const hir_type_base *rsv);
hir_type_base *hir_type_copy(const hir_type_base *generic);

static inline int container_cmp_type(const void *lsv, const void *rsv) {
  return !hir_type_equals(lsv, rsv);
}
static inline void container_delete_type(void *self) {
  return hir_type_free(self);
}
LIST_DECLARE_STATIC_INLINE(list_hir_type, hir_type_base, container_cmp_type,
                           container_new_move, container_delete_type);

int list_hir_type_equals(const list_hir_type *lsv, const list_hir_type *rsv);
list_hir_type *list_hir_type_copy(const list_hir_type *in);

// CUSTOM
typedef struct hir_type_custom_struct {
  hir_type_base  base;
  char          *name;
  list_hir_type *templates;
} hir_type_custom;

hir_type_custom *hir_type_custom_new(span *span, char *name,
                                     list_hir_type *templates);

// ARRAY
typedef struct hir_type_array_struct {
  hir_type_base  base;
  hir_type_base *elem_type;
} hir_type_array;

hir_type_array *hir_type_array_new(span *span, hir_type_base *elem_type);

// CLASS
typedef struct hir_type_class_struct {
  hir_type_base  base;
  char          *name;
  list_hir_type *templates;
  list_hir_type *parents;
} hir_type_class;

hir_type_class *hir_type_class_new(span *span, char *name,
                                   list_hir_type *templates,
                                   list_hir_type *parents);
