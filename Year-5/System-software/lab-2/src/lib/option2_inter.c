#include "option2_inter.h"

#include "Option2Inter.h"
#include "exception.h"
#include "util/list_funcs.h"
#include "util/macro.h"
#include "option2_ast.h"
#include "option2_inter/functions.h"

#include <stdlib.h>

// move
static void *sub_list_new(void *_data) {
  option2_inter_subroutine *exc = _data;
  return exc;
}

static void sub_list_delete(void *_data) {
  if (_data != NULL) {
    option2_inter_subroutine *exc = _data;
    option2_inter_subroutine_free(exc);
  }
}

// move
static void *exc_list_new(void *_data) {
  exception *exc = _data;
  return exc;
}

static void exc_list_delete(void *_data) {
  if (_data != NULL) {
    exception *exc = _data;
    exc->free(exc);
  }
}

static void inter_free(option2_inter *inter) {
  if (inter != NULL) {
    inter->subroutines->free(inter->subroutines);
    inter->exceptions->free(inter->exceptions);
    free(inter);
  }
}

static option2_inter *inter_new() {
  option2_inter *inter = MALLOC(option2_inter);
  inter->subroutines =
      list_new(list_false_cmp, sub_list_new, sub_list_delete, 0);
  inter->exceptions =
      list_new(list_false_cmp, exc_list_new, exc_list_delete, 0);
  inter->free = inter_free;
  return inter;
}

option2_inter *option2_inter_build(const p_list ast) {
  option2_inter *inter = inter_new();

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
