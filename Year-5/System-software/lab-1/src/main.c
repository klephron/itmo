#include <stdio.h>

#include <getopt.h>

#include "Option2Lexer.h"
#include "Option2Parser.h"

struct args_struct {
  char *input_file;
  char *output_file;
  char *prog_name;
  int code;
  int help;
};

void usage(char *prog_name) {
  printf("Usage: %s -i INPUT_FILE -o OUTPUT_FILE\n", prog_name);
}

struct args_struct parse_args(int argc, char *argv[]) {
  struct args_struct args = {.prog_name = argv[0],
                             .code = 0,
                             .help = 0,
                             .input_file = NULL,
                             .output_file = NULL};

  int has_next = 1;

  while (has_next) {
    unsigned c;
    static struct option long_options[] = {};
    int option_index = 0;

    c = getopt_long(argc, argv, "i:o:h", long_options, &option_index);

    if (c == EOF) {
      has_next = 0;
      continue;
    }

    switch (c) {
    case 0:
      if (long_options[option_index].flag != 0) {
        break;
      }
      break;
    case 'i':
      args.input_file = optarg;
      break;
    case 'o':
      args.output_file = optarg;
      break;
    case 'h':
      args.help = 1;
      has_next = 0;
      break;
    default:
      args.code = -1;
      args.help = 1;
      has_next = 0;
      break;
    }
  }

  argc -= optind;
  argv += optind;

  if (argc != 0) {
    args.code = -1;
    args.help = 1;
  }

  return args;
}

void execute(struct args_struct args) {
  pANTLR3_INPUT_STREAM input =
      antlr3FileStreamNew((pANTLR3_UINT8)args.input_file, ANTLR3_ENC_UTF8);

  pOption2Lexer lexer = Option2LexerNew(input);
  pANTLR3_COMMON_TOKEN_STREAM tokens =
      antlr3CommonTokenStreamSourceNew(ANTLR3_SIZE_HINT, TOKENSOURCE(lexer));
  pOption2Parser parser = Option2ParserNew(tokens);

  Option2Parser_source_return r = parser->source(parser);
  pANTLR3_BASE_TREE tree = r.tree;
  pANTLR3_STRING string = tree->toStringTree(tree);
  printf("%s\n", string->chars);

  parser->free(parser);
  tokens->free(tokens);
  lexer->free(lexer);
  input->close(input);
}

int main(int argc, char *argv[]) {
  struct args_struct args = parse_args(argc, argv);

  if (args.help) {
    usage(args.prog_name);
  }

  if (!args.help && args.input_file == NULL) {
    printf("%s: INPUT_FILE is not specified\n", args.prog_name);
    args.code = -1;
  }

  if (!args.help && args.output_file == NULL) {
    printf("%s: OUTPUT_FILE is not specified\n", args.prog_name);
    args.code = -1;
  }

  if (args.code || args.help) {
    return args.code;
  }

  execute(args);

  return 0;
}
