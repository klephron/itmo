#include "option2.h"
#include "assert.h"
#include "exception.h"
#include "util/list.h"
#include "util/list_funcs.h"
#include "util/macro.h"
#include <antlr3.h>
#include <antlr3basetree.h>

static void displayRecognitionError(pANTLR3_BASE_RECOGNIZER recognizer,
                                    pANTLR3_UINT8 *_) {
  pANTLR3_EXCEPTION ex;

  char *exc_stream;
  uint32_t exc_type;
  uint32_t exc_line;
  uint32_t exc_offset;
  char *exc_name;
  char exc_msg[256]; // NOTE: limited message size

  if (!(recognizer->type != ANTLR3_TYPE_PARSER &&
        recognizer->type != ANTLR3_TYPE_LEXER)) {
  }

  ex = recognizer->state->exception;

  if (ex->streamName != NULL) {
    exc_stream = (char *)ex->streamName->to8(ex->streamName)->chars;
  } else {
    exc_stream = NULL;
  }

  exc_type = ex->type;
  exc_line = ex->line;
  exc_name = ex->message;

  pANTLR3_COMMON_TOKEN theToken;
  pANTLR3_STRING ttext;

  exc_offset = recognizer->state->exception->charPositionInLine;

  theToken = (pANTLR3_COMMON_TOKEN)(recognizer->state->exception->token);

  if (theToken) {
    ttext = theToken->toString(theToken);

    if (theToken->type == ANTLR3_TOKEN_EOF) {
      snprintf(exc_msg, 255, "at <EOF>");
    } else {
      snprintf(exc_msg, 255, "near %s",
               ttext == NULL ? (pANTLR3_UINT8) "<empty>" : ttext->chars);
    }
  } else {
    if (recognizer->state) {
      snprintf(exc_msg, 255, "error matching from [Line: %d, LinePos: %d]",
               recognizer->state->tokenStartLine,
               recognizer->state->tokenStartCharPositionInLine);
    } else {
      snprintf(exc_msg, 255,
               "error, dumb implementation, may help [Line: %d, LinePos: %d]",
               exc_line, exc_offset);
    }
  }

  exception *exc = exception_new(exc_stream, exc_type, exc_line, exc_offset,
                                 exc_name, exc_msg);

  p_list exc_list = recognizer->state->userp;
  exc_list->put(exc_list, exc);
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

  ast->lexer->pLexer->rec->state->userp = ast->exceptions;
  ast->parser->pParser->rec->state->userp = ast->exceptions;
  ast->result = ast->parser->source(ast->parser);

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
