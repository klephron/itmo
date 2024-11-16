#pragma once

#include "eir/eir.h"
#include "exception/container.h"
#include "hir/hir.h"

typedef struct eir_ctx_struct {
  eir            *eir;
  list_exception *exceptions;
} eir_ctx;

eir_ctx *eir_ctx_new(eir *eir);
void     eir_ctx_reset_exceptions(eir_ctx *self);
void     eir_ctx_free(eir_ctx *self);

void eir_ctx_build(eir_ctx *self, const hir *hir);
