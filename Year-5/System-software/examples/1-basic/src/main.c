#include <antlr3input.h>
#include <antlr3interfaces.h>
#include <stdio.h>

#include "TLexer.h"
#include "TParser.h"

int main(int argc, char *argv[]) {
  // Set up our Lexer and Parser
  pANTLR3_UINT8 stream_name = (pANTLR3_UINT8) "stdio";
  pANTLR3_UINT8 string_stream = (pANTLR3_UINT8) "call foo;";
  pANTLR3_INPUT_STREAM input =
      antlr3StringStreamNew(string_stream, ANTLR3_ENC_8BIT,
                            strlen((char *)string_stream), stream_name);

  pTLexer lexer = TLexerNew(input);
  pANTLR3_COMMON_TOKEN_STREAM tokens =
      antlr3CommonTokenStreamSourceNew(ANTLR3_SIZE_HINT, TOKENSOURCE(lexer));
  pTParser parser = TParserNew(tokens);
  parser->r(parser);

  parser->free(parser);
  tokens->free(tokens);
  lexer->free(lexer);

  input->close(input);
  return 0;
}
