#include "option2_inter.h"

#include "Option2Inter.h"
#include "option2_ast.h"
#include "option2_inter/userp.h"

#include <stdlib.h>

option2_inter *option2_inter_build(const p_list ast) {
  option2_inter *inter = option2_inter_new();

  for (list_iter it = ast->head; it != NULL; it = it->next) {
    option2_ast *ast = it->data;

    pANTLR3_COMMON_TREE_NODE_STREAM nodes =
        antlr3CommonTreeNodeStreamNewTree(ast->result.tree, ANTLR3_SIZE_HINT);
    pOption2Inter walker = Option2InterNew(nodes);

    option2_inter_userp userp = {inter, option2_ast_filename(ast)};

    walker->pTreeParser->rec->state->userp = &userp;
    walker->source(walker);
    walker->pTreeParser->rec->state->userp = NULL;

    walker->free(walker);
    nodes->free(nodes);
  }

  return inter;
}
