#include "dot.h"
#include "util/macro.h"
#include "util/strbuf.h"

dot_string *dot_cg(const mir *mir, const hir *hir, const type_table *type_table,
                   const symbol_table   *symbol_table,
                   const mir_subroutine *sub) {
  UNUSED(mir);
  UNUSED(hir);
  UNUSED(type_table);
  UNUSED(symbol_table);
  UNUSED(sub);

  strbuf *buffer = strbuf_new(0, 0);

  char *chars = strbuf_detach(buffer);
  return dot_string_new(chars, strlen(chars));
}
