#include "type_table.h"

#include "util/macro.h"
#include <stdlib.h>

type_table *type_table_new() {
  type_table *self = MALLOC(type_table);
  return self;
}

void type_table_free(type_table *self) {
  if (self) {
    free(self);
  }
}
