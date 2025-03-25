#include "inst.h"

#include "util/macro.h"

void cg_inst_class_methods(cg_ctx *ctx, list_cg_method_sym *methods) {
  UNUSED(ctx);

  list_cg_method_sym_free(methods);
}

void cg_inst_class_init(cg_ctx *ctx, list_cg_class_sym *class_inits) {
  UNUSED(ctx);

  list_cg_class_sym_free(class_inits);
}
