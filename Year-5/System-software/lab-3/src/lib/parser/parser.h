#pragma once

#include "antlr3/rec/MainParser.h"
#include "ast/ast.h"
#include "lexer/lexer.h"
#include "util/list.h"

typedef struct parser_userp_struct parser_userp;
typedef struct parser_struct       parser;

struct parser_userp_struct {
  list       *exceptions;
  const char *stream_name;
};

struct parser_struct {
  const lexer_token_stream *tokens;
  MainParser               *parser;
  list                     *exceptions;
  parser_userp              userp;
};

parser *parser_new(const lexer_token_stream *tokens);
void    parser_free(parser *self);
ast    *parser_parse(parser *self);
