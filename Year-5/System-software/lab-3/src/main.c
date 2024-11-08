#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>

#include "dot/ir.h"
#include "exception/exception.h"
#include "lexer/lexer.h"
#include "parser/parser.h"
#include "util/container_util.h"
#include "util/file.h"
#include "util/list.h"
#include "util/log.h"
#include "util/macro.h"

#include "exception/exception.h"

struct args_struct {
  char  *prog_name;
  p_list input_files;
  // options
  char  *output_dir;
  int    ignore_errors;
  int    tee;
  int    cfg_add_expr;
  p_list subroutines;
  // status
  int code;
  int help;
};

struct args_struct *args_new() {
  struct args_struct *args = MALLOC(struct args_struct);

  args->prog_name = NULL;
  args->input_files = list_new(container_cmp_chars, container_new_chars,
                               container_delete_chars);

  args->output_dir = ".";
  args->ignore_errors = 0;
  args->cfg_add_expr = 0;
  args->tee = 0;
  args->subroutines = list_new(container_cmp_chars, container_new_chars,
                               container_delete_chars);

  args->code = 0;
  args->help = 0;

  return args;
}

void args_free(struct args_struct *args) {
  list_free(args->input_files);
  list_free(args->subroutines);
  free(args);
}

void usage(struct args_struct *args) {
  int  buf_len = 256;
  char subroutines[buf_len];
  int  offset = 0;
  for (list_it it = args->subroutines->head; it; it = it->next) {
    offset += snprintf(subroutines + offset, buf_len - offset - 1, "%s,",
                       (const char *)it->data);
  }
  if (offset > 0) {
    subroutines[offset - 1] = '\0';
  }

  printf(
      "Usage: %s [options] <file>...\n"
      "Options:\n"
      "--tee            - print to file and to stdout (current: %d)\n"
      "--ignore-errors  - continue execution on errors (current: %d)\n"
      "--cfg-add-expr   - add expressions to control flow graph (current: %d)\n"
      "-o <directory>   - output directory (current: %s)\n"
      "-s <subroutine>  - set or add subroutine for call graph generation"
      "(current: %s)\n",
      args->prog_name, args->tee, args->ignore_errors, args->cfg_add_expr,
      args->output_dir, subroutines);
}

void parse_args(struct args_struct *args, int argc, char *argv[]) {
  if (argc > 0) {
    args->prog_name = argv[0];
  }

  struct option long_options[] = {
      {"tee", no_argument, &args->tee, 1},
      {"ignore-errors", no_argument, &args->ignore_errors, 1},
      {"cfg-add-expr", no_argument, &args->cfg_add_expr, 1},
  };
  int option_index = 0;
  int has_next = 1;

  while (has_next) {
    unsigned c;

    c = getopt_long(argc, argv, "o:s:h", long_options, &option_index);

    switch (c) {
    case EOF:
      has_next = 0;
      break;
    case 0:
      if (long_options[option_index].flag) {
        break;
      }
      break;
    case 'o':
      args->output_dir = optarg;
      break;
    case 's':
      list_push_back(args->subroutines, optarg);
      break;
    case 'h':
      args->help = 1;
      has_next = 0;
      break;
    default:
      args->code = -1;
      args->help = 1;
      has_next = 0;
      break;
    }
  }

  argc -= optind;
  argv += optind;

  for (; argc > 0; --argc, ++argv) {
    const char *file = argv[0];
    list_push_back(args->input_files, (void *)file);
  }

  if (list_size(args->subroutines) == 0) {
    list_push_back(args->subroutines, "main");
  }
}

// filename is not directory
void ensure_dir_exist(const char *filename) {
  char *dir = dirname(filename);
  dir_create_p(dir);
  free(dir);
}

// ast - ext, dot - file extension
// char *get_ast_dot_output(const struct args_struct *args,
//                          const option2_ast        *ast) {
//   char *suffix = ".ast.dot";
//   char *prefix = join_paths(args->output_dir, option2_ast_filename(ast));

//   char *output = malloc(strlen(prefix) + strlen(suffix) + 1);
//   sprintf(output, "%s%s", prefix, suffix);

//   free(prefix);

//   return output;
// }

char *get_c_dot_output(struct args_struct *args, const char *source,
                       const char *func, const char *ext) {
  char *suffix = ".dot";

  char *prefix = join_paths(args->output_dir, source);

  char *output =
      malloc(strlen(prefix) + strlen(func) + strlen(ext) + strlen(suffix) + 3);
  sprintf(output, "%s.%s.%s%s", prefix, func, ext, suffix);

  free(prefix);

  return output;
}

char *get_cfg_dot_output(struct args_struct *args, const char *source,
                         const char *func) {
  return get_c_dot_output(args, source, func, "cfg");
}

char *get_cg_dot_output(struct args_struct *args, const char *source,
                        const char *func) {
  return get_c_dot_output(args, source, func, "cg");
}

int has_subroutine(struct args_struct *args, const char *subroutine) {
  return !!list_find(args->subroutines, subroutine);
}

void print_exceptions(struct args_struct *args, const p_list exceptions) {
  UNUSED(args);
  for (list_it it = exceptions->head; it; it = it->next) {
    char *msg = exception_to_chars(((exception *)it->data));
    fprintf(stderr, "%s\n", msg);
    free(msg);
  }
}

void write_dot(struct args_struct *args, const dot_string *dot,
               const char *output) {
  if (args->tee) {
    printf("%s\n", dot->chars);
  }
  ensure_dir_exist(output);
  FILE *output_fd = fopen(output, "wb");
  fwrite(dot->chars, dot->len, 1, output_fd);
  fclose(output_fd);
}

void container_delete_lexer(void *data) {
  lexer *lexer = data;
  lexer_free(lexer);
}

void container_delete_token_stream(void *data) {
  lexer_token_stream *stream = data;
  lexer_token_stream_free(stream);
}

void container_delete_parser(void *data) {
  parser *parser = data;
  parser_free(parser);
}

void container_delete_ast(void *data) {
  ast *ast = data;
  ast_free(ast);
}

int execute(struct args_struct *args) {
  int error = 0;

  list *sources =
      list_new(container_cmp_false, container_new_move, container_delete_chars);
  list *lexers =
      list_new(container_cmp_false, container_new_move, container_delete_lexer);
  list *streams = list_new(container_cmp_false, container_new_move,
                           container_delete_token_stream);
  list *parsers = list_new(container_cmp_false, container_new_move,
                           container_delete_parser);
  list *asts =
      list_new(container_cmp_false, container_new_move, container_delete_ast);

  // stage: tokenize and parse to AST
  for (list_it it = args->input_files->head; it; it = it->next) {
    const char *path = (const char *)it->data;
    const char *data = read_file(path);

    lexer              *lexer = lexer_new(data, strlen(data), path);
    lexer_token_stream *stream = lexer_token_stream_new(lexer);
    parser             *parser = parser_new(stream);
    ast                *ast = parser_parse(parser);

    if (list_size(lexer->exceptions) != 0) {
      print_exceptions(args, lexer->exceptions);
      error = 1;
    }

    if (list_size(parser->exceptions) != 0) {
      print_exceptions(args, parser->exceptions);
      error = 1;
    }

    list_push_back(asts, ast);
    list_push_back(parsers, parser);
    list_push_back(streams, stream);
    list_push_back(lexers, lexer);
    list_push_back(sources, (char *)data);
  }

  // stage: ast dot
  // if (!error || args->ignore_errors) {
  //   for (list_it it = asts->head; it; it = it->next) {
  //     option2_ast *ast = it->data;

  //     dot_string *dot = dot_ast(ast);
  //     char       *output = get_ast_dot_output(args, ast);
  //     write_dot(args, dot, output);
  //     free(output);
  //     dot_string_free(dot);
  //   }
  // }

  // stage: build cfg + handle exceptions
  // ir_ctx *ir = NULL;

  // if (!error || args->ignore_errors) {
  //   ir = option2_ir_build(asts);
  //   if (list_size(ir->exceptions)) {
  //     print_exceptions(args, ir->exceptions);
  //     error = 1;
  //   }
  // }

  // stage: build cfg for each function
  // if (!error || args->ignore_errors) {
  //   for (hashset_it it = hashset_begin(ir->subroutines); !it.end(&it);
  //        it.next(&it)) {
  //     ir_subroutine *sub = it.get(&it);
  //     const char    *sub_name = ir_subroutine_name(sub);
  //     const char    *source = sub->file_name;

  //     dot_string *dot_cfg = dot_ir_cfg(ir, sub, args->cfg_add_expr);
  //     char       *output_cfg = get_cfg_dot_output(args, source, sub_name);
  //     write_dot(args, dot_cfg, output_cfg);
  //     free(output_cfg);
  //     dot_string_free(dot_cfg);
  //   }
  // }

  // stage: build cg for selected functions
  // if (!error || args->ignore_errors) {
  //   for (list_it it = args->subroutines->head; it; it = it->next) {
  //     const char *sub_name = it->data;

  //     ir_subroutine *sub_args = ir_subroutine_new(
  //         NULL,
  //         ir_signature_new(ir_term_id_new((ir_loc){}, sub_name), NULL, NULL),
  //         NULL, NULL);

  //     hashset_it it = hashset_find(ir->subroutines, sub_args);

  //     if (!it.end(&it)) {
  //       ir_subroutine *sub = it.get(&it);
  //       dot_string    *dot_cg = dot_ir_cg(ir, sub);
  //       char          *output_cg =
  //           get_cg_dot_output(args, sub->file_name, ir_subroutine_name(sub));
  //       write_dot(args, dot_cg, output_cg);
  //       free(output_cg);
  //       dot_string_free(dot_cg);
  //     } else {
  //       info("function %s is not found", sub_name);
  //     }

  //     ir_subroutine_free(sub_args);
  //   }
  // }

  // ir_ctx_free(ir);
  // list_free(asts);

  list_free(asts);
  list_free(parsers);
  list_free(streams);
  list_free(lexers);
  list_free(sources);

  return error && !args->ignore_errors;
}

int main(int argc, char *argv[]) {
  int code;

  struct args_struct *args = args_new();
  parse_args(args, argc, argv);

  if (args->help) {
    usage(args);
  }

  if (!args->help && list_size(args->input_files) == 0) {
    printf("%s: no input files specified\n", args->prog_name);
    args->code = -1;
  }

  if (!args->help && args->code == 0) {
    args->code = execute(args);
  }

  code = args->code;

  args_free(args);

  return code;
}
