#include "option2_ast.h"
#include "assert.h"
#include "exception.h"
#include "util/list.h"
#include "util/list_funcs.h"
#include "util/macro.h"
#include <antlr3.h>
#include <antlr3basetree.h>

struct antlr_userp {
  p_list exceptions;
  pANTLR3_UINT8 file_name;
};

static void displayRecognitionError(pANTLR3_BASE_RECOGNIZER recognizer,
                                    pANTLR3_UINT8 *_) {
  pANTLR3_EXCEPTION ex;

  enum exception_type exc_type;
  int exc_subtype;
  uint32_t exc_line;
  uint32_t exc_offset;
  char *exc_name;
  char exc_msg[256]; // NOTE: limited message size

  if (!(recognizer->type != ANTLR3_TYPE_PARSER &&
        recognizer->type != ANTLR3_TYPE_LEXER)) {
  }

  switch (recognizer->type) {
  case ANTLR3_TYPE_LEXER:
    exc_type = EXCEPTION_LEXER;
    break;
  case ANTLR3_TYPE_PARSER:
    exc_type = EXCEPTION_PARSER;
    break;
  default:
    exc_type = EXCEPTION_UNKNOWN;
  }

  ex = recognizer->state->exception;

  exc_line = ex->line;
  exc_name = ex->message;

  pANTLR3_COMMON_TOKEN theToken;
  pANTLR3_STRING ttext;

  exc_offset = recognizer->state->exception->charPositionInLine;

  theToken = (pANTLR3_COMMON_TOKEN)(recognizer->state->exception->token);

  if (theToken) {
    ttext = theToken->toString(theToken);

    if (theToken->type == ANTLR3_TOKEN_EOF) {
      exc_subtype = EXCEPTION_PARSER_EOF;
      snprintf(exc_msg, 255, "at <EOF>");
    } else {
      exc_subtype = EXCEPTION_PARSER_GENERIC;
      snprintf(exc_msg, 255, "near %s",
               ttext == NULL ? (pANTLR3_UINT8) "<empty>" : ttext->chars);
    }
  } else {
    if (recognizer->state) {
      exc_subtype = EXCEPTION_LEXER_MATCHING;
      snprintf(exc_msg, 255, "error matching from [Line: %d, LinePos: %d]",
               recognizer->state->tokenStartLine,
               recognizer->state->tokenStartCharPositionInLine);
    } else {
      exc_subtype = EXCEPTION_LEXER_UNKNOWN;
      snprintf(exc_msg, 255,
               "error, dumb implementation, may help [Line: %d, LinePos: %d]",
               exc_line, exc_offset);
    }
  }

  struct antlr_userp *userp = recognizer->state->userp;

  exception *exc =
      exception_new(exc_type, exc_subtype, (const char *)userp->file_name,
                    exc_line, exc_offset, exc_name, exc_msg);

  userp->exceptions->put(userp->exceptions, exc);
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

static void option2_ast_free(struct option2_ast *ast) {
  if (!ast) {
    return;
  }

  ast->exceptions->free(ast->exceptions);
  ast->parser->free(ast->parser);
  ast->tokens->free(ast->tokens);
  ast->lexer->free(ast->lexer);
  ast->stream->close(ast->stream);

  free(ast);
}

option2_ast *option2_ast_build(pANTLR3_UINT8 input_file) {
  option2_ast *ast = MALLOC(option2_ast);

  ast->stream = antlr3FileStreamNew(input_file, ANTLR3_ENC_UTF8);
  ast->lexer = Option2LexerNew(ast->stream);
  ast->tokens = antlr3CommonTokenStreamSourceNew(ANTLR3_SIZE_HINT,
                                                 TOKENSOURCE(ast->lexer));
  ast->parser = Option2ParserNew(ast->tokens);
  ast->exceptions = list_new(list_false_cmp, exc_list_new, exc_list_delete, 0);

  ast->lexer->pLexer->rec->displayRecognitionError = displayRecognitionError;
  ast->parser->pParser->rec->displayRecognitionError = displayRecognitionError;

  struct antlr_userp userp = {ast->exceptions, input_file};

  ast->lexer->pLexer->rec->state->userp = &userp;
  ast->parser->pParser->rec->state->userp = &userp;
  ast->result = ast->parser->source(ast->parser);
  ast->lexer->pLexer->rec->state->userp = NULL;
  ast->parser->pParser->rec->state->userp = NULL;

  ast->free = option2_ast_free;

  return ast;
}

static void *list_option2_new(void *data) {
  option2_ast *ast = data;
  return ast;
}

static void list_option2_delete(void *data) {
  option2_ast *ast = data;
  ast->free(ast);
}

p_list option2_ast_build_many(p_list input_files) {
  p_list asts =
      list_new(list_false_cmp, list_option2_new, list_option2_delete, 0);
  for (list_iter it = input_files->head; it != NULL; it = it->next) {
    option2_ast *ast = option2_ast_build(it->data);
    asts->put(asts, ast);
  }
  return asts;
}
