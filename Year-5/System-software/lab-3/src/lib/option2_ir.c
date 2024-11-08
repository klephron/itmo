#include "option2_ir.h"

#include "antlr3/tree/Pass1_GlobalSymbolResolution.h"
#include "antlr3/tree/Pass2_IR.h"
#include "exception.h"
#include "ir/pass.h"
#include "ir/pass1.h"
#include "ir/pass2.h"
#include "option2_ast.h"
#include "util/macro.h"

#include <stdlib.h>

static void displayRecognitionError(pANTLR3_BASE_RECOGNIZER recognizer,
                                    pANTLR3_UINT8          *token) {
  UNUSED(token);

  struct ir_pass_base *pass = recognizer->state->userp;
  exception           *exc = NULL;

  pANTLR3_STRING    ttext;
  pANTLR3_BASE_TREE theBaseTree;

  struct exception_helper exh;
  exh.type = EXCEPTION_TREE;
  exh.subtype = EXCEPTION_TREE_UNEXPECTED_NODE;
  exh.line = 0;
  exh.offset = 0;
  exh.name = recognizer->state->exception->message;

  switch (pass->kind) {
  case IR_PASS_1:
    exh.stream = (char *)((ir_pass1 *)pass)->file_name;
    break;
  case IR_PASS_2:
    exh.stream = (char *)((ir_pass2 *)pass)->file_name;
    break;
  }

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

  switch (pass->kind) {
  case IR_PASS_1:
    ir_ctx_add_exception(((ir_pass1 *)pass)->ctx, exc);
    break;
  case IR_PASS_2:
    ir_ctx_add_exception(((ir_pass2 *)pass)->ctx, exc);
    break;
  }
}

ir_ctx *option2_ir_build(const p_list ast) {
  ir_ctx *ctx = ir_ctx_new();

  debug("----------------------------PASS1----------------------------");
  ir_pass1 pass1_userp;
  ir_pass1_init(&pass1_userp, ctx);

  for (list_it it = ast->head; it; it = it->next) {
    option2_ast *ast = it->data;

    pass1_userp.file_name = option2_ast_filename(ast);

    pANTLR3_COMMON_TREE_NODE_STREAM nodes =
        antlr3CommonTreeNodeStreamNewTree(ast->result.tree, ANTLR3_SIZE_HINT);

    pPass1_GlobalSymbolResolution pass1 = Pass1_GlobalSymbolResolutionNew(nodes);

    pass1->pTreeParser->rec->displayRecognitionError = displayRecognitionError;
    pass1->pTreeParser->rec->state->userp = &pass1_userp;
    pass1->source(pass1);
    pass1->pTreeParser->rec->state->userp = NULL;

    pass1->free(pass1);
    nodes->free(nodes);
  }

  ir_pass1_deinit(&pass1_userp);
  debug("----------------------------PASS1----------------------------");

  debug("----------------------------PASS2----------------------------");
  ir_pass2 pass2_userp;
  ir_pass2_init(&pass2_userp, ctx);

  for (list_it it = ast->head; it; it = it->next) {
    option2_ast *ast = it->data;

    pass2_userp.file_name = option2_ast_filename(ast);

    pANTLR3_COMMON_TREE_NODE_STREAM nodes =
        antlr3CommonTreeNodeStreamNewTree(ast->result.tree, ANTLR3_SIZE_HINT);

    pPass2_IR pass2 = Pass2_IRNew(nodes);

    pass2->pTreeParser->rec->displayRecognitionError = displayRecognitionError;
    pass2->pTreeParser->rec->state->userp = &pass2_userp;
    pass2->source(pass2);
    pass2->pTreeParser->rec->state->userp = NULL;

    pass2->free(pass2);
    nodes->free(nodes);
  }

  ir_pass2_deinit(&pass2_userp);
  debug("----------------------------PASS2----------------------------");

  return ctx;
}
