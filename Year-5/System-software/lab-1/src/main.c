#include <stdio.h>

#include "ExampleLexer.h"
#include "ExampleParser.h"

int main(int argc, char* argv[]) {
  pANTLR3_UINT8 filename = (pANTLR3_UINT8) argv[1];
  printf("%s\n", argv[1]);
  // Set up our Lexer and Parser
  pANTLR3_INPUT_STREAM input;
  pExampleLexer lex;
  pANTLR3_COMMON_TOKEN_STREAM tokens;
  pExampleParser parser;
  // Get the input from the file
  input = antlr3FileStreamNew(filename, ANTLR3_ENC_8BIT);
  // Create the lexer using the input stream
  lex = ExampleLexerNew(input);
  // Create the token stream using the lexer
  tokens = antlr3CommonTokenStreamSourceNew(ANTLR3_SIZE_HINT, TOKENSOURCE(lex));
  // Create the parser using the token stream
  parser = ExampleParserNew(tokens);
  // Parse the program from the top level rule (expr)
  ExampleParser_expr_return r = parser->expr(parser);
  // // Create the parse tree
  pANTLR3_BASE_TREE tree = r.tree;
  pANTLR3_STRING string = tree->toStringTree(tree);
  printf("%s\n", string->chars);

  printf("%d\n", tree->getChildCount(tree));
  for (int i = 0; i < tree->getChildCount(tree); ++i) {
    pANTLR3_BASE_TREE child = (pANTLR3_BASE_TREE) tree->getChild(tree, i);
    pANTLR3_STRING string = child->toStringTree(child);
    printf("%s\n", string->chars);
  }
  // // Do what you want with the parse tree now
  // // Remember to free things, C style
  parser->free(parser);
  tokens->free(tokens);
  lex->free(lex);
  input->close(input);
  return 0;
}
