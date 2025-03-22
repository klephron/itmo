#include "inst_sub.h"

#include "util/strbuf.h"
#include <string.h>

char *cg_label_sub_global(const mir_subroutine *sub) {
  return strdup(sub->symbol_ref->name);
}

char *cg_label_sub_global_suf(const mir_subroutine *sub, const char *suffix) {
  strbuf *buffer = strbuf_new(64, 0);
  strbuf_append(buffer, ".L");
  strbuf_append(buffer, sub->symbol_ref->name);
  strbuf_append(buffer, "_");
  strbuf_append(buffer, suffix);
  return strbuf_detach(buffer);
}

char *cg_label_sub_global_bb(const mir_subroutine *sub, const mir_bb *bb) {
  char    buf[64];
  strbuf *buffer = strbuf_new(64, 0);
  strbuf_append(buffer, ".L");
  strbuf_append(buffer, sub->symbol_ref->name);
  strbuf_append_f(buffer, buf, "_bb%lu", bb->id);
  return strbuf_detach(buffer);
}

uint64_t cg_aligned(uint64_t size) {
  return ((size - 1) / CG_X86_64_SIZE_ALIGN + 1) * CG_X86_64_SIZE_ALIGN;
}
