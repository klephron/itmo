#include <getopt.h>
#include <stdio.h>

#include "dot.h"
#include "option2.h"

struct args_struct {
  char *input_file;
  char *output_file;
  char *prog_name;
  int tee;
  int code;
  int help;
};

void usage(char *prog_name) {
  printf("Usage: %s -i INPUT_FILE -o OUTPUT_FILE [--tee]\n", prog_name);
}

struct args_struct parse_args(int argc, char *argv[]) {
  struct args_struct args = {.prog_name = argv[0],
                             .code = 0,
                             .help = 0,
                             .tee = 0,
                             .input_file = NULL,
                             .output_file = NULL};

  struct option long_options[] = {
      {"tee", no_argument, &args.tee, 1},
  };
  int option_index = 0;

  int has_next = 1;

  while (has_next) {
    unsigned c;

    c = getopt_long(argc, argv, "i:o:h", long_options, &option_index);

    switch (c) {
    case EOF:
      has_next = 0;
      break;
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
  pANTLR3_INPUT_STREAM stream;
  pOption2Lexer lexer;
  pANTLR3_COMMON_TOKEN_STREAM tokens;
  pOption2Parser parser;

  pANTLR3_BASE_TREE tree = option2_ast((pANTLR3_UINT8)args.input_file, &stream,
                                       &lexer, &tokens, &parser);

  dot_string dot_string = dot_ast(tree);

  // print to stdout
  if (args.tee) {
    printf("%s\n", dot_string.chars);
  }
  // write to output file
  if (args.output_file != NULL) {
    FILE *output_fd = fopen(args.output_file, "wb");
    fwrite(dot_string.chars, dot_string.len, 1, output_fd);
    fclose(output_fd);
  }

  free((void *)dot_string.chars);

  parser->free(parser);
  tokens->free(tokens);
  lexer->free(lexer);
  stream->close(stream);
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
