#include "inst_sub.h"

#include "util/strbuf.h"
#include <string.h>

char *cg_sub_global_label(const mir_subroutine *sub) {
  return strdup(sub->symbol_ref->name);
}

char *cg_sub_global_label_suf(const mir_subroutine *sub, const char *suffix) {
  strbuf *buffer = strbuf_new(64, 0);
  strbuf_append(buffer, ".L");
  strbuf_append(buffer, sub->symbol_ref->name);
  strbuf_append(buffer, "_");
  strbuf_append(buffer, suffix);
  return strbuf_detach(buffer);
}

size_t cg_aligned(size_t size, size_t align) {
  return ((size - 1) / align + 1) * align;
}
