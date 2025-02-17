#include "type_table.h"

#include "util/macro.h"
#include <stdlib.h>

type_entry *type_entry_new(char *id, type_base *type, span *span) {
  type_entry *self = MALLOC(type_entry);
  self->id = id;
  self->type = type;
  self->span = span;
  return self;
}

void type_entry_free(type_entry *self) {
  if (self) {
    free(self->id);
    type_free(self->type);
    span_free(self->span);
    free(self);
  }
}

type_table *type_table_new() {
  type_table *self = MALLOC(type_table);
  self->entries = hashset_type_entry_new();
  return self;
}

void type_table_free(type_table *self) {
  if (self) {
    hashset_type_entry_free(self->entries);
    free(self);
  }
}
