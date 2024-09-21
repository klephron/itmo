#pragma once

#include "option2.h"

pANTLR3_BASE_TREE option2_ast(pANTLR3_UINT8 input_file,
                              pANTLR3_INPUT_STREAM *stream_out,
                              pOption2Lexer *lexer_out,
                              pANTLR3_COMMON_TOKEN_STREAM *tokens_out,
                              pOption2Parser *parser_out) {
  pANTLR3_INPUT_STREAM input = antlr3FileStreamNew(input_file, ANTLR3_ENC_UTF8);
  pOption2Lexer lexer = Option2LexerNew(input);
  pANTLR3_COMMON_TOKEN_STREAM tokens =
      antlr3CommonTokenStreamSourceNew(ANTLR3_SIZE_HINT, TOKENSOURCE(lexer));
  pOption2Parser parser = Option2ParserNew(tokens);

  Option2Parser_source_return r = parser->source(parser);

  *parser_out = parser;
  *tokens_out = tokens;
  *stream_out = input;
  *lexer_out = lexer;

  return r.tree;
}
