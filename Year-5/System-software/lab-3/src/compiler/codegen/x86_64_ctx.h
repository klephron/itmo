#pragma once

#include "compiler/codegen/x86_64.h"
#include "compiler/exception/container.h"
#include "compiler/mir/mir.h"

typedef struct cg_x86_64_ctx_struct {
  cg_x86_64      *cg_ref;
  list_exception *exceptions;
} cg_x86_64_ctx;

cg_x86_64_ctx *cg_x86_64_ctx_new(cg_x86_64 *cg_ref);
void           cg_x86_64_ctx_free(cg_x86_64_ctx *self);
void           cg_x86_64_ctx_build(cg_x86_64_ctx *self, mir *mir);
char          *cg_x86_64_ctx_emit(cg_x86_64_ctx *self);
