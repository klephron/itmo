#include "ctx.h"
#include "assert.h"
#include "compiler/mir/mir.h"
#include "util/log.h"
#include "util/macro.h"
#include <string.h>

// MAPPINGS
mir_map_sub *mir_map_sub_new(const hir_symbol *hir_ref,
                             mir_node_base    *mir_ref) {
  mir_map_sub *self = MALLOC(mir_map_sub);
  self->hir_ref = hir_ref;
  self->mir_ref = mir_ref;
  return self;
}

void mir_map_sub_free(mir_map_sub *self) { free(self); }

mir_map_value *mir_map_value_new(const hir_symbol *hir_ref,
                                 mir_value        *mir_ref) {
  mir_map_value *self = MALLOC(mir_map_value);
  self->hir_ref = hir_ref;
  self->mir_ref = mir_ref;
  return self;
}

void mir_map_value_free(mir_map_value *self) { free(self); }

mir_map_bb *mir_map_bb_new(const eir_block_base *hir_ref, mir_bb *mir_ref) {
  mir_map_bb *self = MALLOC(mir_map_bb);
  self->hir_ref = hir_ref;
  self->mir_ref = mir_ref;
  return self;
}

void mir_map_bb_free(mir_map_bb *self) { free(self); }

// CTX
mir_ctx *mir_ctx_new(mir *mir_ref) {
  mir_ctx *self = MALLOC(mir_ctx);
  self->mir_ref = mir_ref;
  self->exceptions = list_exception_new();
  self->value_cnt = 0;
  self->map_subs = hashset_mir_map_sub_new();
  return self;
}

void mir_ctx_free(mir_ctx *self) {
  if (self) {
    self->mir_ref = NULL;
    list_exception_free(self->exceptions);
    self->value_cnt = 0;
    hashset_mir_map_sub_free(self->map_subs);
    free(self);
  }
}

// ARGS
void *container_new_mir_arg(void *data) { return container_new_move(data); }
void  container_delete_mir_arg(void *data) {
   if (!data) {
     return;
  }
   mir_node_base *base = data;
   switch (base->kind) {
   case MIR_NODE_VALUE:
   case MIR_NODE_SUBROUTINE:
   case MIR_NODE_SUBROUTINE_EXTERNAL:
    (void)data;
    break;
   case MIR_NODE_LIT:
   case MIR_NODE_BUILTIN:
    mir_node_free(data);
    break;
   default:
    error("unexpected node type %d %p", base->kind, base);
  }
}
