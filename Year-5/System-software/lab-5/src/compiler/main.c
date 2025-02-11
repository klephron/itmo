#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>

#include "compiler/ast/ast.h"
#include "compiler/codegen/x86_64/ctx.h"
#include "compiler/codegen/x86_64/x86_64.h"
#include "compiler/dot/ast.h"
#include "compiler/dot/eir.h"
#include "compiler/eir/ctx.h"
#include "compiler/exception/exception.h"
#include "compiler/hir/ctx.h"
#include "compiler/hir/str.h"
#include "compiler/mir/ctx.h"
#include "compiler/mir/str.h"
#include "util/container_util.h"
#include "util/file.h"
#include "util/list.h"
#include "util/macro.h"

LIST_DECLARE_STATIC_INLINE(list_chars, char, container_cmp_chars,
                           container_new_move, container_delete_chars);

typedef struct args_struct {
  char       *prog_name;
  list_chars *input_files;
  // options
  char       *output_dir;
  char       *output_file;
  int         ignore_errors;
  int         tee;
  int         ast;
  int         cfg;
  int         cfg_add_expr;
  int         cg;
  list_chars *cg_subroutines;
  int         hir_symbols;
  int         hir_tree;
  int         mir;
  // status
  int code;
  int help;
} args;

static args *args_new() {
  args *args = MALLOC(struct args_struct);

  args->prog_name = NULL;
  args->input_files = list_chars_new();

  args->output_dir = ".";
  args->output_file = "a.asm";
  args->ignore_errors = 0;
  args->tee = 0;

  args->ast = 0;

  args->cfg = 0;
  args->cfg_add_expr = 0;
  args->cg = 0;
  args->cg_subroutines = list_chars_new();

  args->hir_symbols = 0;
  args->hir_tree = 0;

  args->mir = 0;

  args->code = 0;
  args->help = 0;

  return args;
}

static void args_free(args *args) {
  list_chars_free(args->input_files);
  list_chars_free(args->cg_subroutines);
  free(args);
}

static void usage(args *args) {
  int  buf_len = 256;
  char cg_subroutines[buf_len];
  int  offset = 0;
  for (list_chars_it it = list_chars_begin(args->cg_subroutines); !it.end(&it);
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
      "-d <directory>   - output directory (current: %s)\n"
      "-o <file>        - main output file (current: %s)\n"
      "--tee            - print to file and to stdout (current: %d)\n"
      "--ignore-errors  - continue execution on errors (current: %d)\n"
      "--ast            - add AST output (current: %d)\n"
      "--cfg            - add control flow graph output (current: %d)\n"
      "--cfg-add-expr   - add expressions to control flow graph (current: %d)\n"
      "--cg             - add call graph output (current: %d)\n"
      "-s <subroutine>  - set or add subroutine for call graph generation"
      "(current: %s)\n"
      "--hir-symbols    - print HIR symbols (current: %d)\n"
      "--hir-tree       - print HIR tree (current: %d)\n"
      "--mir            - print MIR tree (current: %d)\n"
      "-h               - show help\n",
      args->prog_name, args->output_dir, args->output_file, args->tee,
      args->ignore_errors, args->ast, args->cfg, args->cfg_add_expr, args->cg,
      cg_subroutines, args->hir_symbols, args->hir_tree, args->mir);
}

static void parse(args *args, int argc, char *argv[]) {
  int           option_index;
  int           has_next;
  struct option long_options[] = {
      {"tee", no_argument, &args->tee, 1},
      {"ignore-errors", no_argument, &args->ignore_errors, 1},
      {"ast", no_argument, &args->ast, 1},
      {"cfg", no_argument, &args->cfg, 1},
      {"cfg-add-expr", no_argument, &args->cfg_add_expr, 1},
      {"cg", no_argument, &args->cg, 1},
      {"hir-symbols", no_argument, &args->hir_symbols, 1},
      {"hir-tree", no_argument, &args->hir_tree, 1},
      {"mir", no_argument, &args->mir, 1},
  };

  if (argc > 0) {
    args->prog_name = argv[0];
  }

  option_index = 0;
  has_next = 1;

  while (has_next) {
    unsigned c;

    c = getopt_long(argc, argv, "o:d:s:h", long_options, &option_index);

    switch (c) {
    case EOF:
      has_next = 0;
      break;
    case 0:
      if (long_options[option_index].flag) {
        break;
      }
      break;
    case 'd':
      args->output_dir = optarg;
      break;
    case 'o':
      args->output_file = optarg;
      break;
    case 's':
      list_chars_push_back(args->cg_subroutines, strdup(optarg));
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
    char *file = argv[0];
    list_chars_push_back(args->input_files, realpath(file, NULL));
  }

  if (list_chars_size(args->cg_subroutines) == 0) {
    list_chars_push_back(args->cg_subroutines, strdup("main"));
  }
}

// filename is not directory
static void ensure_dir_exist(const char *filename) {
  char *dir = dirname(filename);
  dir_create_p(dir);
  free(dir);
}

// ast - ext, dot - file extension
static char *get_ast_dot_path(const args *args, const ast *ast) {
  char *cwd = getcwd(NULL, 0);
  char *suffix = ".ast.dot";

  const char *fpath = ast->name + strlen(cwd) + 1;
  char       *prefix = join_paths(args->output_dir, fpath);

  char *output = malloc(strlen(prefix) + strlen(suffix) + 1);
  sprintf(output, "%s%s", prefix, suffix);

  free(prefix);
  free(cwd);
  return output;
}

static char *get_c_dot_path(args *args, const char *source, const char *func,
                            const char *ext) {
  char *cwd = getcwd(NULL, 0);
  char *suffix = ".dot";

  const char *fpath = source + strlen(cwd) + 1;
  char       *prefix = join_paths(args->output_dir, fpath);

  char *output =
      malloc(strlen(prefix) + strlen(func) + strlen(ext) + strlen(suffix) + 3);
  sprintf(output, "%s.%s.%s%s", prefix, func, ext, suffix);

  free(prefix);
  free(cwd);
  return output;
}

static char *get_cfg_dot_path(args *args, const char *source,
                              const char *func) {
  return get_c_dot_path(args, source, func, "cfg");
}

static char *get_cg_dot_path(args *args, const char *source, const char *func) {
  return get_c_dot_path(args, source, func, "cg");
}

static char *get_asm_path(args *args, const char *source) {
  char *path = join_paths(args->output_dir, source);
  return path;
}

static int has_subroutine(args *args, const char *subroutine) {
  list_chars_it it = list_chars_find(args->cg_subroutines, subroutine);
  return !it.end(&it);
}

static void print_exceptions(args *args, list_exception *exceptions) {
  UNUSED(args);
  if (!exceptions) {
    return;
  }
  for (list_exception_it it = list_exception_begin(exceptions); !it.end(&it);
       it.next(&it)) {
    exception *exc = it.get(&it);
    char      *msg = exception_str(exc);
    fprintf(stderr, "%s\n", msg);
    free(msg);
  }
}

static void write_dot(args *args, const dot_string *dot, const char *path) {
  if (args->tee) {
    printf("%s\n", dot->chars);
  }
  ensure_dir_exist(path);
  if (path) {
    FILE *output_fd = fopen(path, "wb");
    fwrite(dot->chars, dot->len, 1, output_fd);
    fclose(output_fd);
  }
}

static void write_asm(args *args, const char *data, const char *path) {
  if (args->tee) {
    printf("%s\n", data);
  }
  ensure_dir_exist(path);
  if (path) {
    FILE *output_fd = fopen(path, "wb");
    fwrite(data, strlen(data), 1, output_fd);
    fclose(output_fd);
  }
}

static inline void container_delete_ast(void *data) {
  ast *ast = data;
  ast_free(ast);
}
LIST_DECLARE_STATIC_INLINE(list_ast, ast, container_cmp_false,
                           container_new_move, container_delete_ast);

static int execute(args *args) {
  list_ast *asts = list_ast_new();

  // stage: tokenize and parse to AST
  for (list_chars_it it = list_chars_begin(args->input_files); !it.end(&it);
       it.next(&it)) {
    const char *path = it.get(&it);
    char       *source = read_file(path);

    ast *ast = ast_build(path, source);

    if (list_exception_count_by_level(ast_lexer_exceptions(ast),
                                      EXCEPTION_LEVEL_ERROR) ||
        list_exception_count_by_level(ast_parser_exceptions(ast),
                                      EXCEPTION_LEVEL_ERROR)) {
      args->code = 1;
    }
    print_exceptions(args, ast_lexer_exceptions(ast));
    print_exceptions(args, ast_parser_exceptions(ast));

    list_ast_push_back(asts, ast);
  }

  // stage: dot ast
  if (args->ast && (!args->code || args->ignore_errors)) {
    for (list_ast_it it = list_ast_begin(asts); !it.end(&it); it.next(&it)) {
      ast        *ast = it.get(&it);
      dot_string *dot = dot_ast(ast);
      char       *output = get_ast_dot_path(args, ast);
      write_dot(args, dot, output);
      free(output);
      dot_string_free(dot);
    }
  }

  // stage: build hir + handle exceptions
  hir     *hir = hir_new();
  hir_ctx *hir_ctx = hir_ctx_new(hir);

  if (!args->code || args->ignore_errors) {
    for (list_ast_it it = list_ast_begin(asts); !it.end(&it); it.next(&it)) {
      const ast *ast = list_ast_it_get(&it);
      hir_ctx_build(hir_ctx, ast);
    }
    if (list_exception_count_by_level(hir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      args->code = 1;
    }
    print_exceptions(args, hir_ctx->exceptions);
    hir_ctx_reset_exceptions(hir_ctx);
  }

  // stage: hir bind symbols
  if (!args->code || args->ignore_errors) {
    hir_ctx_bind(hir_ctx);
    if (list_exception_count_by_level(hir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      args->code = 1;
    }
    print_exceptions(args, hir_ctx->exceptions);
    hir_ctx_reset_exceptions(hir_ctx);
  }

  // stage: hir type
  if (!args->code || args->ignore_errors) {
    hir_ctx_type(hir_ctx);
    if (list_exception_count_by_level(hir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      args->code = 1;
    }
    print_exceptions(args, hir_ctx->exceptions);
    hir_ctx_reset_exceptions(hir_ctx);
  }

  // stage: print hir symbols
  if (args->hir_symbols && (!args->code || args->ignore_errors)) {
    char *globals = hir_list_symbol_str(hir->globals);
    printf("HIR GLOBAL SYMBOLS:\n%s", globals);
    free(globals);

    char *locals = hir_list_symbol_str(hir->locals);
    printf("HIR LOCAL SYMBOLS:\n%s", locals);
    free(locals);
  }

  // stage: print hir tree
  if (args->hir_tree && (!args->code || args->ignore_errors)) {
    char *tree = hir_tree_str(hir);
    printf("HIR TREE:\n%s\n", tree);
    free(tree);
  }

  eir     *eir = eir_new();
  eir_ctx *eir_ctx = eir_ctx_new(eir);

  // stage: build eir
  if (!args->code || args->ignore_errors) {
    eir_ctx_build(eir_ctx, hir);
    if (list_exception_count_by_level(eir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      args->code = 1;
    }
    print_exceptions(args, eir_ctx->exceptions);
    eir_ctx_reset_exceptions(eir_ctx);
  }

  // stage: dot cfg
  if (args->cfg && (!args->code || args->ignore_errors)) {
    for (list_eir_subroutine_it it =
             list_eir_subroutine_begin(eir->subroutines);
         !it.end(&it); it.next(&it)) {
      const eir_subroutine *sub = it.get(&it);
      const char           *sub_name = sub->binding_ref->name;
      const char           *source = sub->binding_ref->decl_span->source_ref;

      dot_string *dot = dot_eir_cfg(eir, sub, args->cfg_add_expr);
      char       *output = get_cfg_dot_path(args, source, sub_name);
      write_dot(args, dot, output);
      free(output);
      dot_string_free(dot);
    }
  }

  // stage: dot cg
  if (args->cg && (!args->code || args->ignore_errors)) {
    for (list_eir_subroutine_it it =
             list_eir_subroutine_begin(eir->subroutines);
         !it.end(&it); it.next(&it)) {
      const eir_subroutine *sub = it.get(&it);
      const char           *sub_name = sub->binding_ref->name;
      const char           *source = sub->binding_ref->decl_span->source_ref;

      if (has_subroutine(args, sub_name)) {
        dot_string *dot = dot_eir_cg(eir, sub);
        char       *output = get_cg_dot_path(args, source, sub_name);
        write_dot(args, dot, output);
        free(output);
        dot_string_free(dot);
      }
    }
  }

  mir     *mir = mir_new();
  mir_ctx *mir_ctx = mir_ctx_new(mir);

  // stage: build mir
  if (!args->code || args->ignore_errors) {
    mir_ctx_build(mir_ctx, eir);
    // optimizations
    mir_ctx_bb_merge(mir_ctx);
    if (list_exception_count_by_level(mir_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      args->code = 1;
    }
    print_exceptions(args, mir_ctx->exceptions);
  }

  // stage: print mir tree
  if (args->mir && (!args->code || args->ignore_errors)) {
    char *tree = mir_tree_str(mir);
    printf("MIR TREE:\n%s\n", tree);
    free(tree);
  }

  cg_x86_64     *cg = cg_x86_64_new();
  cg_x86_64_ctx *cg_ctx = cg_x86_64_ctx_new(cg);

  // stage: build x86_64 structs
  if (!args->code || args->ignore_errors) {
    cg_x86_64_ctx_build(cg_ctx, mir);
    if (list_exception_count_by_level(cg_ctx->exceptions,
                                      EXCEPTION_LEVEL_ERROR)) {
      args->code = 1;
    }
    print_exceptions(args, cg_ctx->exceptions);
  }

  // stage: emit x86_64 assembly code
  if (!args->code || args->ignore_errors) {
    char *data = cg_x86_64_ctx_emit(cg_ctx);
    if (data) {
      char *path = get_asm_path(args, args->output_file);
      write_asm(args, data, path);
      free(path);
      free(data);
    }
  }

  cg_x86_64_ctx_free(cg_ctx);
  cg_x86_64_free(cg);

  mir_ctx_free(mir_ctx);
  mir_free(mir);

  eir_ctx_free(eir_ctx);
  eir_free(eir);

  hir_ctx_free(hir_ctx);
  hir_free(hir);

  list_ast_free(asts);

  return args->code && !args->ignore_errors;
}

int main(int argc, char *argv[]) {
  int code;

  args *args = args_new();
  parse(args, argc, argv);

  if (args->help) {
    usage(args);
  }

  if (!args->help && list_chars_size(args->input_files) == 0) {
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
