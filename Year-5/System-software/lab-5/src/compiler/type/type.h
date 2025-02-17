#pragma once

#include "util/container_util.h"
#include "util/list.h"

typedef enum type_enum {
  TYPE_UNKNOWN,
  TYPE_PRIMITIVE,
  TYPE_ARRAY,
  TYPE_CALLABLE,
  TYPE_CLASS,
  TYPE_TYPENAME,
  TYPE_SPECIALIZED,
} type_enum;

typedef enum type_spec_enum {
  TYPE_SPEC_UNSET,
  TYPE_SPEC_FULL,
  TYPE_SPEC_NOT_FULL
} type_spec_enum;

typedef struct type_base_struct {
  type_enum kind;
  // maybe add type_entry_ref?
} type_base;

void           type_free(type_base *generic);
type_spec_enum type_calc_spec(const type_base *generic);
type_spec_enum type_get_spec(const type_base *generic);

static inline void container_delete_type(void *data) { type_free(data); }
LIST_DECLARE_STATIC_INLINE(list_type, type_base, container_cmp_false,
                           container_new_move, container_delete_type);

LIST_DECLARE_STATIC_INLINE(list_type_ref, type_base, container_cmp_false,
                           container_new_move, container_delete_false);

typedef enum type_primitive_enum {
  TYPE_PRIMITIVE_UNKNOWN,
  TYPE_PRIMITIVE_BOOL,
  TYPE_PRIMITIVE_BYTE,
  TYPE_PRIMITIVE_INT,
  TYPE_PRIMITIVE_UINT,
  TYPE_PRIMITIVE_LONG,
  TYPE_PRIMITIVE_ULONG,
  TYPE_PRIMITIVE_CHAR,
  TYPE_PRIMITIVE_STRING,
  TYPE_PRIMITIVE_VOID,
} type_primitive_enum;

typedef struct type_primitive_struct {
  type_base           base;
  type_primitive_enum type;
} type_primitive;

type_primitive *type_primitive_new(type_primitive_enum type);

// NOTE:
// * full specialization - all templates match actual types (recursively)
// * partial specialization - (a = C<int, T>) or even (a = C<int, B<T>>)
// * calculation of `spec` is not done inside constructor (cuz it may fail)
//
typedef struct type_array_struct {
  type_base      base;
  type_base     *element_ref;
  type_spec_enum spec;
} type_array;

type_array *type_array_new(type_base *element_ref);

typedef struct type_callable_struct {
  type_base      base;
  type_base     *ret_ref;
  list_type_ref *params;
  type_spec_enum spec;
} type_callable;

type_callable *type_callable_new(type_base *ret_ref, list_type_ref *params);

// primary template
typedef struct type_class_struct {
  type_base      base;
  list_type_ref *typenames;
  list_type_ref *parents;
  type_spec_enum spec;
} type_class;

type_class *type_class_new(list_type_ref *typenames, list_type_ref *parents);

typedef struct type_typename_struct {
  type_base base;
  // reference to class for ex. which declared this typename
  char      *id;
  type_base *source_ref;
} type_typename;

type_typename *type_typename_new(char *id, type_base *source_ref);

// if type_ref=class templates go in order as typenames of class
typedef struct type_specialized_struct {
  type_base base;
  // list sizes should be the same.
  type_base     *type_ref;
  list_type_ref *templates;
  list_type_ref *specs;
  type_spec_enum spec;
} type_specialized;

type_specialized *type_specialized_new(type_base     *type_ref,
                                       list_type_ref *templates,
                                       list_type_ref *specs);
