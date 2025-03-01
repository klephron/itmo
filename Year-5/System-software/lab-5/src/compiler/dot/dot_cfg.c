#include "dot.h"
#include "util/macro.h"
#include "util/strbuf.h"

dot_string *dot_cfg(const mir *mir, const hir *hir,
                    const type_table   *type_table,
                    const symbol_table *symbol_table, const mir_subroutine *sub,
                    int option_add_expr) {
  UNUSED(mir);
  UNUSED(hir);
  UNUSED(type_table);
  UNUSED(symbol_table);
  UNUSED(sub);
  UNUSED(option_add_expr);

  strbuf *buffer = strbuf_new(0, 0);

  char *chars = strbuf_detach(buffer);
  return dot_string_new(chars, strlen(chars));
}
