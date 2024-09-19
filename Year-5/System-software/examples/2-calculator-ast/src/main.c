#include <antlr3input.h>
#include <antlr3interfaces.h>
#include <stdio.h>

#include "Eval.h"
#include "ExprLexer.h"
#include "ExprParser.h"

#include "util/list.h"

int main_test(int argc, char *argv[]);
int main_antlr(int argc, char *argv[]);

int main(int argc, char *argv[]) {
  main_antlr(argc, argv);
  return 0;
}

int main_antlr(int argc, char *argv[]) {
  // ANTLR3_UINT8 stdio_buffer[1024];
  // if (fgets((char *)stdio_buffer, sizeof(stdio_buffer), stdin) == NULL) {
  //   fprintf(stderr, "Failed to read from stdin.\n");
  //   return EXIT_FAILURE;
  // }

  pANTLR3_UINT8 string_buffer = (pANTLR3_UINT8) "a=(3+4)*5\nb=(a*5)\n(a-b)\n";

  pANTLR3_UINT8 stream_name = (pANTLR3_UINT8) "stream";
  pANTLR3_INPUT_STREAM input =
      antlr3StringStreamNew(string_buffer, ANTLR3_ENC_8BIT,
                            strlen((char *)string_buffer), stream_name);

  pExprLexer lexer = ExprLexerNew(input);
  pANTLR3_COMMON_TOKEN_STREAM tokens =
      antlr3CommonTokenStreamSourceNew(ANTLR3_SIZE_HINT, TOKENSOURCE(lexer));
  pExprParser parser = ExprParserNew(tokens);

  ExprParser_prog_return res = parser->prog(parser);

  pANTLR3_BASE_TREE tree = res.tree;
  pANTLR3_COMMON_TREE_NODE_STREAM nodes = antlr3CommonTreeNodeStreamNewTree(tree, ANTLR3_SIZE_HINT);

  pEval walker = EvalNew(nodes);
  walker->prog(walker);

  walker->free(walker);
  nodes->free(nodes);
  parser->free(parser);
  tokens->free(tokens);
  lexer->free(lexer);

  input->close(input);
  return 0;
}

int main_test(int argc, char *argv[]) {
  result_int res;
  p_list list = list_new();

  res = list->get(list, "first");
  printf("code %d, value %d\n", res.code, res.value);

  list->put(list, "first", 5);
  res = list->get(list, "first");
  printf("code %d, value %d\n", res.code, res.value);

  list->put(list, "second", 10);
  res = list->get(list, "second");
  printf("code %d, value %d\n", res.code, res.value);

  list->put(list, "first", 8);
  res = list->get(list, "first");
  printf("code %d, value %d\n", res.code, res.value);

  list->put(list, "first", 8);
  res = list->get(list, "first");
  printf("code %d, value %d\n", res.code, res.value);

  list->free(list);
  return 0;
}
