#include "ctx.h"

#include "antlr3/tree/HirBuilder.h"
#include "util/macro.h"

hir_ctx *hir_ctx_new(const hir *hir) {
  hir_ctx *self = MALLOC(hir_ctx);
  self->hir = hir;
  self->exceptions = list_exception_new();
  self->ast_stream = NULL;
  return self;
}

void hir_ctx_free(hir_ctx *self) {
  if (self) {
    self->hir = NULL;
    list_exception_free(self->exceptions);
    self->exceptions = NULL;
    self->ast_stream = NULL;
    free(self);
  }
}

static void hir_display_error(pANTLR3_BASE_RECOGNIZER recognizer,
                              pANTLR3_UINT8          *token) {
  UNUSED(token);

  hir_ctx   *ctx = recognizer->state->userp;
  exception *exc = NULL;

  pANTLR3_STRING    ttext;
  pANTLR3_BASE_TREE theBaseTree;

  exception_helper exh;
  exh.stream = (char *)ctx->ast_stream;
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

void hir_build_from_ast(hir_ctx *ctx, const ast *ast) {
  ctx->ast_stream = ast->name;

  pANTLR3_COMMON_TREE_NODE_STREAM nodes =
      antlr3CommonTreeNodeStreamNewTree(ast->tree, ANTLR3_SIZE_HINT);
  HirBuilder *builder = HirBuilderNew(nodes);

  builder->pTreeParser->rec->state->userp = ctx;
  builder->pTreeParser->rec->displayRecognitionError = hir_display_error;

  builder->source(builder);

  builder->free(builder);
  nodes->free(nodes);
}
