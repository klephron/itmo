#include "ctx.h"

#include "compiler/antlr3/tree/HirBuilder.h"
#include "compiler/exception/container.h"
#include "util/macro.h"

// SCOPE
hir_ctx_scope_entry *hir_ctx_scope_entry_new(char       *name_ref,
                                             hir_symbol *binding_ref) {
  hir_ctx_scope_entry *self = MALLOC(hir_ctx_scope_entry);
  self->name_ref = name_ref;
  self->binding_ref = binding_ref;
  return self;
}

void hir_ctx_scope_entry_free(hir_ctx_scope_entry *self) {
  if (self) {
    free(self);
  }
}

hir_ctx_scope *hir_ctx_scope_new(hir_ctx_scope_enum kind) {
  hir_ctx_scope *self = MALLOC(hir_ctx_scope);
  self->kind = kind;
  self->hashset = hashset_hir_ctx_scope_entry_new();
  return self;
}

void hir_ctx_scope_free(hir_ctx_scope *self) {
  if (self) {
    hashset_hir_ctx_scope_entry_free(self->hashset);
    free(self);
  }
}

// CTX
hir_ctx *hir_ctx_new(hir *hir_ref) {
  hir_ctx *self = MALLOC(hir_ctx);
  self->hir_ref = hir_ref;
  self->exceptions = list_exception_new();
  self->ast_stream_ref = NULL;
  self->scopes = NULL;
  return self;
}

void hir_ctx_reset_exceptions(hir_ctx *self) {
  list_exception_free(self->exceptions);
  self->exceptions = list_exception_new();
}

void hir_ctx_free(hir_ctx *self) {
  if (self) {
    self->hir_ref = NULL;
    list_exception_free(self->exceptions);
    self->exceptions = NULL;
    self->ast_stream_ref = NULL;
    self->scopes = NULL;
    free(self);
  }
}

// BUILD
static void hir_ctx_build_display_error(pANTLR3_BASE_RECOGNIZER recognizer,
                                        pANTLR3_UINT8          *token) {
  UNUSED(token);

  hir_ctx   *ctx = recognizer->state->userp;
  exception *exc = NULL;

  pANTLR3_STRING    ttext;
  pANTLR3_BASE_TREE theBaseTree;

  exception_helper exh;
  exh.level = EXCEPTION_LEVEL_ERROR;
  exh.stream = (char *)ctx->ast_stream_ref;
  exh.type = EXCEPTION_TREE;
  exh.subtype = EXCEPTION_TREE_UNEXPECTED_NODE;
  exh.line = 0;
  exh.offset = 0;
  exh.name = recognizer->state->exception->message;

  switch (recognizer->type) {
  case ANTLR3_TYPE_TREE_PARSER:
    theBaseTree = (pANTLR3_BASE_TREE)(recognizer->state->exception->token);
    ttext = theBaseTree->toStringTree(theBaseTree);

    if (theBaseTree != NULL) {
      exc = exception_new_from_helper(
          &exh, "at offset %d near %s",
          theBaseTree->getCharPositionInLine(theBaseTree), ttext->chars);
    }
    break;
  }

  if (!exc) {
    return;
  }

  list_exception_push_back(ctx->exceptions, exc);
}

void hir_ctx_build(hir_ctx *ctx, const ast *ast) {
  ctx->ast_stream_ref = ast->name_ref;

  pANTLR3_COMMON_TREE_NODE_STREAM nodes =
      antlr3CommonTreeNodeStreamNewTree(ast->tree, ANTLR3_SIZE_HINT);
  HirBuilder *builder = HirBuilderNew(nodes);

  builder->pTreeParser->rec->state->userp = ctx;
  builder->pTreeParser->rec->displayRecognitionError =
      hir_ctx_build_display_error;

  builder->source(builder);

  builder->free(builder);
  nodes->free(nodes);

  ctx->ast_stream_ref = NULL;
}
