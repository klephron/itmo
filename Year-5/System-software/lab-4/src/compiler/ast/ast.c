#include "ast.h"
#include "util/macro.h"

ast *ast_new(ANTLR3_BASE_TREE *tree, const char *name) {
  ast *self = MALLOC(ast);
  self->tree = tree;
  self->name = name;
  return self;
}

void ast_free(ast *self) {
  if (self) {
    self->tree = NULL;
    free(self);
  }
}
