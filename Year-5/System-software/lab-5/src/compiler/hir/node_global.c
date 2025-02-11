#include "node_global.h"

#include "compiler/hir/node.h"
#include "util/macro.h"

// SUBROUTINE
hir_subroutine *hir_subroutine_new(span *span, hir_id *id,
                                   list_hir_param *params,
                                   hir_type_base *ret_type, list_hir_var *vars,
                                   void *body) {
  hir_subroutine *self = MALLOC(hir_subroutine);
  hir_base_init(&self->base, span, HIR_NODE_SUBROUTINE);
  self->id = id;
  self->params = params;
  self->ret_type = ret_type;
  self->vars = vars;
  self->body = body;
  return self;
}

void hir_subroutine_free(hir_subroutine *self) {
  if (self) {
    hir_id_free(self->id);
    list_hir_param_free(self->params);
    hir_type_free(self->ret_type);
    list_hir_var_free(self->vars);
    hir_stmt_block_free(self->body);
    hir_base_deinit(&self->base);
    free(self);
  }
}
