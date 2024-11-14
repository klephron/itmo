#pragma once

#include "ast/ast.h"
#include "exception/container.h"
#include "hir/hir.h"

typedef struct hir_ctx_struct {
  const hir      *hir;
  list_exception *exceptions;
  const char     *ast_stream;
} hir_ctx;

hir_ctx *hir_ctx_new(const hir *hir);
void     hir_ctx_reset_exceptions(hir_ctx *self);
void     hir_ctx_free(hir_ctx *self);

void hir_build_from_ast(hir_ctx *ctx, const ast *ast);
void hir_type(hir_ctx *ctx);
