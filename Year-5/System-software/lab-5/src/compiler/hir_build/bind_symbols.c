#include "bind_symbols.h"

hir_bind_symbols_result hir_bind_symbols(hir *hir) {
  hir_bind_symbols_result result = {
      .symbol_table = symbol_table_new(),
      .exceptions = list_exception_new(),
  };

  return result;
}
