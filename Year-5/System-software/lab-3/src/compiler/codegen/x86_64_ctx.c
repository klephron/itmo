#include "x86_64_ctx.h"

#include "util/macro.h"

cg_x86_64_map_value *cg_x86_64_map_value_new(const mir_value *value_ref,
                                             size_t           rbp_offset) {
  cg_x86_64_map_value *self = MALLOC(cg_x86_64_map_value);
  self->value_ref = value_ref;
  self->rbp_offset = rbp_offset;
  return self;
}

void cg_x86_64_map_value_free(cg_x86_64_map_value *self) {
  if (self) {
    free(self);
  }
}

cg_x86_64_ctx *cg_x86_64_ctx_new(cg_x86_64 *cg_ref) {
  cg_x86_64_ctx *self = MALLOC(cg_x86_64_ctx);
  self->cg_ref = cg_ref;
  self->exceptions = list_exception_new();
  return self;
}

void cg_x86_64_ctx_free(cg_x86_64_ctx *self) {
  if (self) {
    self->cg_ref = NULL;
    list_exception_free(self->exceptions);
    free(self);
  }
}
