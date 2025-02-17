#pragma once

#include "compiler/span/span.h"
#include "compiler/type_table/type.h"

typedef struct type_entry_struct {
  char      *id;
  type_base *type;
  span      *span;
} type_entry;

typedef struct type_table_struct {

} type_table;

type_table *type_table_new();
void        type_table_free(type_table *self);
