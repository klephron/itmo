#pragma once

#include "compiler/eir/eir.h"
#include "compiler/exception/container.h"
#include "compiler/hir/hir.h"

LIST_DECLARE_STATIC_INLINE(list_eir_block, eir_block_base, container_cmp_false,
                           container_new_move, container_delete_false);

typedef struct eir_scope_struct {
  eir_block_base *begin;
  eir_block_base *cur;
  eir_block_base *end;
} eir_scope;

eir_scope *eir_scope_new(eir_block_base *begin, eir_block_base *cur,
                         eir_block_base *end);
void       eir_scope_free(eir_scope *self);

static inline void container_delete_eir_scope(void *data) {
  eir_scope_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_eir_scope, eir_scope, container_cmp_false,
                           container_new_move, container_delete_eir_scope);

typedef struct eir_ctx_struct {
  eir            *eir_ref;
  list_exception *exceptions;
  // internal data
  list_eir_block *break_stack;
  list_eir_scope *scope_stack;
  size_t          block_cnt;
} eir_ctx;

eir_ctx *eir_ctx_new(eir *eir_ref);
void     eir_ctx_reset_exceptions(eir_ctx *self);
void     eir_ctx_free(eir_ctx *self);

void eir_ctx_build(eir_ctx *self, const hir *hir);
