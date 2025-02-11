#pragma once

#include "compiler/span/span.h"

typedef struct symbol_entry_struct {
  char                 *name;
  struct symbol_struct *bind_parent_ref;
  span                 *decl_span;
} symbol_table_entry;

typedef struct symbol_table_struct {

} symbol_table;

symbol_table *symbol_table_new();
void          symbol_table_free(symbol_table *self);
