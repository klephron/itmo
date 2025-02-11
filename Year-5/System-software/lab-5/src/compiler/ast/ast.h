#pragma once

#include "compiler/exception/container.h"
#include "compiler/lexer/lexer.h"
#include "compiler/parser/parser.h"
#include <antlr3basetree.h>

typedef struct ast_struct {
  const char         *name_ref;
  char               *source;
  lexer              *lexer;
  lexer_token_stream *token_stream;
  parser             *parser;
  ANTLR3_BASE_TREE   *tree;
} ast;

ast *ast_build(const char *name_ref, char *source);
void ast_free(ast *self);

list_exception *ast_lexer_exceptions(ast *self);
list_exception *ast_parser_exceptions(ast *self);
