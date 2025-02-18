#include "bind_symbols.h"

hir_bind_symbols_result hir_bind_symbols(hir *hir) {
  return (hir_bind_symbols_result){
      .symbol_table = NULL,
      .exceptions = NULL,
  };
}
