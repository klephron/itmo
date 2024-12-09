#pragma once

#include "compiler/antlr3/rec/MainParser.h"
#include "compiler/ast/ast.h"
#include "compiler/lexer/lexer.h"

typedef struct parser_userp_struct {
  list_exception *exceptions;
  const char     *stream_name;
} parser_userp;

typedef struct parser_struct {
  const lexer_token_stream *tokens;
  MainParser               *parser;
  list_exception           *exceptions;
  parser_userp              userp;
} parser;

parser *parser_new(const lexer_token_stream *tokens);
void    parser_free(parser *self);
ast    *parser_parse(parser *self);
