#include "option2_ast.h"
#include "assert.h"
#include "exception.h"
#include "util/container_funcs.h"
#include "util/list.h"
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

  struct exception_helper exh;

  if (!(recognizer->type != ANTLR3_TYPE_PARSER &&
        recognizer->type != ANTLR3_TYPE_LEXER)) {
  }

  switch (recognizer->type) {
  case ANTLR3_TYPE_LEXER:
    exh.type = EXCEPTION_LEXER;
    break;
  case ANTLR3_TYPE_PARSER:
    exh.type = EXCEPTION_PARSER;
    break;
  default:
    exh.type = EXCEPTION_UNKNOWN;
  }

  ex = recognizer->state->exception;

  exh.line = ex->line;
  exh.name = ex->message;

  pANTLR3_COMMON_TOKEN theToken;
  pANTLR3_STRING ttext;

  exh.offset = recognizer->state->exception->charPositionInLine;

  theToken = (pANTLR3_COMMON_TOKEN)(recognizer->state->exception->token);
  exception *exc;

  struct antlr_userp *userp = recognizer->state->userp;
  exh.stream = (char *)userp->file_name;

  if (theToken) {
    ttext = theToken->toString(theToken);

    if (theToken->type == ANTLR3_TOKEN_EOF) {
      exh.subtype = EXCEPTION_PARSER_EOF;

      exc = exception_new_from_helper(&exh, "at <EOF>");
    } else {
      exh.subtype = EXCEPTION_PARSER_GENERIC;
      exc = exception_new_from_helper(&exh, "near %s",
                                      ttext == NULL ? (pANTLR3_UINT8) "<empty>"
                                                    : ttext->chars);
    }
  } else {
    if (recognizer->state) {
      exh.subtype = EXCEPTION_LEXER_MATCHING;
      exc = exception_new_from_helper(
          &exh, "error matching from [Line: %d, LinePos: %d]",
          recognizer->state->tokenStartLine,
          recognizer->state->tokenStartCharPositionInLine);

    } else {
      exh.subtype = EXCEPTION_LEXER_UNKNOWN;
      exc = exception_new_from_helper(
          &exh, "error, dumb implementation, may help [Line: %d, LinePos: %d]",
          exh.line, exh.offset);
    }
  }

  userp->exceptions->put(userp->exceptions, exc);
}

// move
static void *container_new_exc(void *_data) {
  exception *exc = _data;
  return exc;
}

static void container_delete_exc(void *_data) {
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
  ast->exceptions =
      list_new(container_cmp_false, container_new_exc, container_delete_exc, 0);

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

static void *container_new_option2(void *data) {
  option2_ast *ast = data;
  return ast;
}

static void container_delete_option2(void *data) {
  option2_ast *ast = data;
  ast->free(ast);
}

p_list option2_ast_build_many(p_list input_files) {
  p_list asts = list_new(container_cmp_false, container_new_option2,
                         container_delete_option2, 0);
  for (list_iter it = input_files->head; it != NULL; it = it->next) {
    option2_ast *ast = option2_ast_build(it->data);
    asts->put(asts, ast);
  }
  return asts;
}

const char *option2_ast_filename(const option2_ast *ast) {
  return (const char *)ast->stream->fileName->chars;
}
