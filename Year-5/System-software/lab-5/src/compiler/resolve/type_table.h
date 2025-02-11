#pragma once

typedef struct type_entry_struct {
  char *name;
} type_entry;

typedef struct type_table_struct {

} type_table;

type_table *type_table_new();
void        type_table_free(type_table *self);
