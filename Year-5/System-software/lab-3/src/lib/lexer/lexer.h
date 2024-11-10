#pragma once

#include "antlr3/rec/MainLexer.h"
#include "exception/container.h"
#include <antlr3interfaces.h>

typedef struct lexer_userp_struct        lexer_userp;
typedef struct lexer_struct              lexer;
typedef struct lexer_token_stream_struct lexer_token_stream;

struct lexer_userp_struct {
  list_exception *exceptions;
  const char     *name;
};

struct lexer_struct {
  ANTLR3_INPUT_STREAM *stream;
  MainLexer           *lexer;
  list_exception      *exceptions;
  lexer_userp          userp;
};

struct lexer_token_stream_struct {
  pANTLR3_COMMON_TOKEN_STREAM tokens;
  const char                 *name;
};

lexer *lexer_new(const char *data, size_t len, const char *name);
void   lexer_free(lexer *);

lexer_token_stream *lexer_token_stream_new(lexer *self);
void                lexer_token_stream_free(lexer_token_stream *self);
const char         *lexer_get_input_file(lexer *self);
