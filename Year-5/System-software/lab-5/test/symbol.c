#include <criterion/criterion.h>

#include "compiler/hir/symbols.h"
#include "util/log.h"
#include "util/string_buffer.h"

static void hir_binding_parent_str(string_buffer    *buffer,
                                   const hir_symbol *parent) {
  if (parent) {
    hir_binding_parent_str(buffer, parent->bind_parent_ref);
    if (parent->bind_parent_ref) {
      string_buffer_append(buffer, "::");
    }
    string_buffer_append(buffer, parent->name);
  }
}

Test(symbol, test) {
  hir_symbol *symbol1 =
      hir_symbol_new(strdup("root"), HIR_BINDING_GLOBAL, NULL, NULL, NULL);
  hir_symbol *symbol2 =
      hir_symbol_new(strdup("child"), HIR_BINDING_GLOBAL, symbol1, NULL, NULL);
  string_buffer *buffer = string_buffer_new(0, 0);
  hir_binding_parent_str(buffer, symbol2);
  char *data = string_buffer_detach(buffer);
  debug("%s", data);
  free(data);
  hir_symbol_free(symbol2);
  hir_symbol_free(symbol1);
}
