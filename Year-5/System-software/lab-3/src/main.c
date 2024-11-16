#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>

#include "dot/ast.h"
#include "eir/ctx.h"
#include "exception/exception.h"
#include "hir/ctx.h"
#include "hir/str.h"
#include "lexer/lexer.h"
#include "parser/parser.h"
#include "util/container_util.h"
#include "util/file.h"
#include "util/list.h"
#include "util/macro.h"

LIST_DECLARE_STATIC_INLINE(list_char, char, container_cmp_chars,
                           container_new_chars, container_delete_chars);

struct args_struct {
  char      *prog_name;
  list_char *input_files;
  // options
  char      *output_dir;
  int        ignore_errors;
  int        tee;
  int        ast;
  int        cfg;
  int        cfg_add_expr;
  int        cg;
  list_char *cg_subroutines;
  int        hir_symbols;
  int        hir_tree;
  // status
  int code;
  int help;
};

struct args_struct *args_new() {
  struct args_struct *args = MALLOC(struct args_struct);

  args->prog_name = NULL;
  args->input_files = list_char_new();

  args->output_dir = ".";
  args->ignore_errors = 0;
  args->tee = 0;

  args->ast = 0;

  args->cfg = 0;
  args->cfg_add_expr = 0;
  args->cg = 0;
  args->cg_subroutines = list_char_new();

  args->hir_symbols = 0;
  args->hir_tree = 0;

  args->code = 0;
  args->help = 0;

  return args;
}

void args_free(struct args_struct *args) {
  list_char_free(args->input_files);
  list_char_free(args->cg_subroutines);
  free(args);
}

void usage(struct args_struct *args) {
  int  buf_len = 256;
  char cg_subroutines[buf_len];
  int  offset = 0;
  for (list_char_it it = list_char_begin(args->cg_subroutines); !it.end(&it);
       it.next(&it)) {
    char *sub = it.get(&it);
    offset +=
        snprintf(cg_subroutines + offset, buf_len - offset - 1, "%s,", sub);
  }
  if (offset > 0) {
    cg_subroutines[offset - 1] = '\0';
  }

  printf(
      "Usage: %s [options] <file>...\n"
      "Options:\n"
      "-o <directory>   - output directory (current: %s)\n"
      "--tee            - print to file and to stdout (current: %d)\n"
      "--ignore-errors  - continue execution on errors (current: %d)\n"
      "--ast            - add AST output (current: %d)\n"
      "--cfg            - add control flow graph output (current: %d)\n"
      "--cfg-add-expr   - add expressions to control flow graph (current: %d)\n"
      "--cg             - add call graph output (current: %d)\n"
      "-s <subroutine>  - set or add subroutine for call graph generation"
      "(current: %s)\n"
      "--hir-symbols    - print HIR symbols (current: %d)\n"
      "--hir-tree       - print HIR tree (current: %d)\n",
      args->prog_name, args->output_dir, args->tee, args->ignore_errors,
      args->ast, args->cfg, args->cfg_add_expr, args->cg, cg_subroutines,
      args->hir_symbols, args->hir_tree);
}

void parse_args(struct args_struct *args, int argc, char *argv[]) {
  if (argc > 0) {
    args->prog_name = argv[0];
  }

  struct option long_options[] = {
      {"tee", no_argument, &args->tee, 1},
      {"ignore-errors", no_argument, &args->ignore_errors, 1},
      {"ast", no_argument, &args->ast, 1},
      {"cfg", no_argument, &args->cfg, 1},
      {"cfg-add-expr", no_argument, &args->cfg_add_expr, 1},
      {"cg", no_argument, &args->cg, 1},
      {"hir-symbols", no_argument, &args->hir_symbols, 1},
      {"hir-tree", no_argument, &args->hir_tree, 1},
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
      list_char_push_back(args->cg_subroutines, optarg);
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
    list_char_push_back(args->input_files, (void *)file);
  }

  if (list_char_size(args->cg_subroutines) == 0) {
    list_char_push_back(args->cg_subroutines, "main");
  }
}

// filename is not directory
void ensure_dir_exist(const char *filename) {
  char *dir = dirname(filename);
  dir_create_p(dir);
  free(dir);
}

// ast - ext, dot - file extension
char *get_ast_dot_output(const struct args_struct *args, const ast *ast) {
  char *suffix = ".ast.dot";
  char *prefix = join_paths(args->output_dir, ast->name);

  char *output = malloc(strlen(prefix) + strlen(suffix) + 1);
  sprintf(output, "%s%s", prefix, suffix);

  free(prefix);
  return output;
}

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
  list_char_it it = list_char_find(args->cg_subroutines, subroutine);
  return !list_char_it_end(&it);
}

void print_exceptions(struct args_struct *args, list_exception *exceptions) {
  UNUSED(args);
  for (list_exception_it it = list_exception_begin(exceptions); !it.end(&it);
       it.next(&it)) {
    exception *exc = it.get(&it);
    char      *msg = exception_str(exc);
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

static inline void container_delete_lexer(void *data) {
  lexer *lexer = data;
  lexer_free(lexer);
}
LIST_DECLARE_STATIC_INLINE(list_lexer, lexer, container_cmp_false,
                           container_new_move, container_delete_lexer);

static inline void container_delete_token_stream(void *data) {
  lexer_token_stream *stream = data;
  lexer_token_stream_free(stream);
}
LIST_DECLARE_STATIC_INLINE(list_stream, lexer_token_stream, container_cmp_false,
                           container_new_move, container_delete_token_stream);

static inline void container_delete_parser(void *data) {
  parser *parser = data;
  parser_free(parser);
}
LIST_DECLARE_STATIC_INLINE(list_parser, parser, container_cmp_false,
                           container_new_move, container_delete_parser);

static inline void container_delete_ast(void *data) {
  ast *ast = data;
  ast_free(ast);
}
LIST_DECLARE_STATIC_INLINE(list_ast, ast, container_cmp_false,
                           container_new_move, container_delete_ast);

LIST_DECLARE_STATIC_INLINE(list_source, char, container_cmp_false,
                           container_new_move, container_delete_chars);

int execute(struct args_struct *args) {
  int error = 0;

  list_source *sources = list_source_new();
  list_lexer  *lexers = list_lexer_new();
  list_stream *streams = list_stream_new();
  list_parser *parsers = list_parser_new();
  list_ast    *asts = list_ast_new();

  // stage: tokenize and parse to AST
  for (list_char_it it = list_char_begin(args->input_files); !it.end(&it);
       it.next(&it)) {
    const char *path = it.get(&it);
    const char *data = read_file(path);

    lexer              *lexer = lexer_new(data, strlen(data), path);
    lexer_token_stream *stream = lexer_token_stream_new(lexer);
    parser             *parser = parser_new(stream);
    ast                *ast = parser_parse(parser);

    if (list_exception_count_by_level(lexer->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      error = 1;
    }
    print_exceptions(args, lexer->exceptions);

    if (list_exception_count_by_level(parser->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      error = 1;
    }
    print_exceptions(args, parser->exceptions);

    list_ast_push_back(asts, ast);
    list_parser_push_back(parsers, parser);
    list_stream_push_back(streams, stream);
    list_lexer_push_back(lexers, lexer);
    list_source_push_back(sources, (char *)data);
  }

  // stage: dot ast
  if (args->ast && (!error || args->ignore_errors)) {
    for (list_ast_it it = list_ast_begin(asts); !it.end(&it); it.next(&it)) {
      ast        *ast = it.get(&it);
      dot_string *dot = dot_ast(ast);
      char       *output = get_ast_dot_output(args, ast);
      write_dot(args, dot, output);
      free(output);
      dot_string_free(dot);
    }
  }

  // stage: build hir + handle exceptions
  hir     *hir = hir_new();
  hir_ctx *hir_ctx = hir_ctx_new(hir);

  if (!error || args->ignore_errors) {
    for (list_ast_it it = list_ast_begin(asts); !it.end(&it); it.next(&it)) {
      const ast *ast = list_ast_it_get(&it);
      hir_ctx_build(hir_ctx, ast);
    }
    if (list_exception_count_by_level(hir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      error = 1;
    }
    print_exceptions(args, hir_ctx->exceptions);
    hir_ctx_reset_exceptions(hir_ctx);
  }

  // stage: hir bind symbols
  if (!error || args->ignore_errors) {
    hir_ctx_bind(hir_ctx);
    if (list_exception_count_by_level(hir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      error = 1;
    }
    print_exceptions(args, hir_ctx->exceptions);
    hir_ctx_reset_exceptions(hir_ctx);
  }

  // stage: hir type
  if (!error || args->ignore_errors) {
    hir_ctx_type(hir_ctx);
    if (list_exception_count_by_level(hir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      error = 1;
    }
    print_exceptions(args, hir_ctx->exceptions);
    hir_ctx_reset_exceptions(hir_ctx);
  }

  // stage: print hir symbols
  if (args->hir_symbols && (!error || args->ignore_errors)) {
    char *globals = hir_list_symbol_str(hir->globals);
    printf("HIR GLOBAL SYMBOLS:\n%s", globals);
    free(globals);

    char *locals = hir_list_symbol_str(hir->locals);
    printf("HIR LOCAL SYMBOLS:\n%s", locals);
    free(locals);
  }

  // stage: print hir tree
  if (args->hir_tree && (!error || args->ignore_errors)) {
    char *tree = hir_tree_str(hir);
    printf("HIR TREE:\n%s\n", tree);
    free(tree);
  }

  eir     *eir = eir_new();
  eir_ctx *eir_ctx = eir_ctx_new(eir);

  // stage: build mir + handle exception
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

  // stage: dot cfg
  // stage: dot cg
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
  eir_ctx_free(eir_ctx);
  eir_free(eir);

  hir_ctx_free(hir_ctx);
  hir_free(hir);

  list_ast_free(asts);
  list_parser_free(parsers);
  list_stream_free(streams);
  list_lexer_free(lexers);
  list_source_free(sources);

  return error && !args->ignore_errors;
}

int main(int argc, char *argv[]) {
  int code;

  struct args_struct *args = args_new();
  parse_args(args, argc, argv);

  if (args->help) {
    usage(args);
  }

  if (!args->help && list_char_size(args->input_files) == 0) {
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
