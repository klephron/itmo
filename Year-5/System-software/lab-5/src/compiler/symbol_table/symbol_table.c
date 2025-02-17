#include "symbol_table.h"

#include "util/macro.h"
#include <stdlib.h>

symbol_table *symbol_table_new() {
  symbol_table *self = MALLOC(symbol_table);
  return self;
}

void symbol_table_free(symbol_table *self) {
  if (self) {
    free(self);
  }
}
