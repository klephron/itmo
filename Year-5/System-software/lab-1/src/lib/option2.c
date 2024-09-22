#include "option2.h"
#include "assert.h"
#include "exception.h"
#include "util/list.h"
#include "util/list_funcs.h"

static void displayRecognitionError(pANTLR3_BASE_RECOGNIZER recognizer,
                                    pANTLR3_UINT8 *_) {
  pANTLR3_EXCEPTION ex;

  char *exc_stream;
  uint32_t exc_type;
  uint32_t exc_line;
  uint32_t exc_offset;
  char *exc_name;
  char exc_msg[256]; // NOTE: limited message size
                     //

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
  exc_list->put(exc_list, NULL, exc);
}

// move
static list_data *exc_list_new(const void *key, const void *value) {
  list_data *data = malloc(sizeof(list_data));
  data->key = (void *)key;
  data->value = (void *)value;
  return data;
}

static void exc_list_delete(list_data *data) {
  if (data != NULL) {
    struct exception_struct *exc = data->value;
    exc->free(exc);
    free(data);
  }
}

pANTLR3_BASE_TREE
option2_ast(pANTLR3_UINT8 input_file, pANTLR3_INPUT_STREAM *stream_out,
            pOption2Lexer *lexer_out, pANTLR3_COMMON_TOKEN_STREAM *tokens_out,
            pOption2Parser *parser_out, p_list *exc_list_out) {
  pANTLR3_INPUT_STREAM input = antlr3FileStreamNew(input_file, ANTLR3_ENC_UTF8);
  pOption2Lexer lexer = Option2LexerNew(input);
  pANTLR3_COMMON_TOKEN_STREAM tokens =
      antlr3CommonTokenStreamSourceNew(ANTLR3_SIZE_HINT, TOKENSOURCE(lexer));
  pOption2Parser parser = Option2ParserNew(tokens);

  lexer->pLexer->rec->displayRecognitionError = displayRecognitionError;
  parser->pParser->rec->displayRecognitionError = displayRecognitionError;

  p_list exc_list =
      list_new(list_equals_false, exc_list_new, exc_list_delete, 0);

  lexer->pLexer->rec->state->userp = exc_list;
  parser->pParser->rec->state->userp = exc_list;
  Option2Parser_source_return r = parser->source(parser);
  parser->pParser->rec->state->userp = NULL;
  lexer->pLexer->rec->state->userp = NULL;

  *parser_out = parser;
  *tokens_out = tokens;
  *stream_out = input;
  *lexer_out = lexer;
  *exc_list_out = exc_list;

  return r.tree;
}
