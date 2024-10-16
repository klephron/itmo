#include "option2_ir.h"

#include "Option2Pass1_Declarations.h"
#include "Option2Pass2_IR.h"
#include "ir/userp.h"
#include "option2_ast.h"

#include <stdlib.h>

ir_ctx *option2_ir_build(const p_list ast) {
  ir_ctx *ctx = ir_ctx_new();

  for (list_iter it = ast->head; it != NULL; it = it->next) {
    option2_ast *ast = it->data;

    pANTLR3_COMMON_TREE_NODE_STREAM nodes =
        antlr3CommonTreeNodeStreamNewTree(ast->result.tree, ANTLR3_SIZE_HINT);

    ir_userp userp = {ctx, option2_ast_filename(ast)};

    pOption2Pass1_Declarations pass1 = Option2Pass1_DeclarationsNew(nodes);
    pass1->pTreeParser->rec->state->userp = &userp;
    pass1->source(pass1);
    pass1->pTreeParser->rec->state->userp = NULL;

    pOption2Pass2_IR pass2 = Option2Pass2_IRNew(nodes);

    pass2->pTreeParser->rec->state->userp = &userp;
    pass2->source(pass2);
    pass2->pTreeParser->rec->state->userp = NULL;

    pass2->free(pass2);
    pass1->free(pass1);
    nodes->free(nodes);
  }

  return ctx;
}
