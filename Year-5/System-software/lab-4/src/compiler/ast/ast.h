#pragma once

#include <antlr3basetree.h>

struct ast_struct {
  ANTLR3_BASE_TREE *tree;
  const char       *name;
};

typedef struct ast_struct ast;

ast *ast_new(ANTLR3_BASE_TREE *tree, const char *name);
void ast_free(ast *self);
