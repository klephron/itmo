#include "inst.h"

#include "util/strbuf.h"

char *cg_label_local_suf(const char *base, const char *suffix) {
  strbuf *buffer = strbuf_new(64, 0);
  strbuf_append(buffer, ".L");
  strbuf_append(buffer, base);
  strbuf_append(buffer, "_");
  strbuf_append(buffer, suffix);
  return strbuf_detach(buffer);
}

char *cg_label_local_bb(const char *base, const mir_bb *bb) {
  char    buf[64];
  strbuf *buffer = strbuf_new(64, 0);
  strbuf_append(buffer, ".L");
  strbuf_append(buffer, base);
  strbuf_append_f(buffer, buf, "_bb%lu", bb->id);
  return strbuf_detach(buffer);
}

uint64_t cg_aligned(uint64_t size) {
  return ((size - 1) / CG_X86_64_SIZE_ALIGN + 1) * CG_X86_64_SIZE_ALIGN;
}
