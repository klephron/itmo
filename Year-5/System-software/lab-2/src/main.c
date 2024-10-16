#include <getopt.h>
#include <stdio.h>

#include "dot_ir.h"
#include "util/file.h"
#include "util/list.h"
#include "util/list_funcs.h"
#include "util/macro.h"

#include "dot_ast.h"
#include "exception.h"
#include "option2_ast.h"
#include "option2_ir.h"

struct args_struct {
  char *prog_name;
  p_list input_files;
  // options
  char *output_dir;
  int ignore_errors;
  int tee;
  p_list subroutines;
  // status
  int code;
  int help;
};

struct args_struct *args_new() {
  struct args_struct *args = MALLOC(struct args_struct);

  args->prog_name = NULL;
  args->input_files =
      list_new(list_chars_cmp, list_chars_new, list_chars_delete, 0);

  args->output_dir = ".";
  args->ignore_errors = 0;
  args->tee = 0;
  args->subroutines =
      list_new(list_chars_cmp, list_chars_new, list_chars_delete, 0);

  args->code = 0;
  args->help = 0;

  return args;
}

void args_free(struct args_struct *args) {
  args->input_files->free(args->input_files);
  args->subroutines->free(args->subroutines);
  free(args);
}

void usage(struct args_struct *args) {
  int buf_len = 256;
  char subroutines[buf_len];
  int offset = 0;
  for (list_iter it = args->subroutines->head; it != NULL; it = it->next) {
    offset += snprintf(subroutines + offset, buf_len - offset - 1, "%s,",
                       (const char *)it->data);
  }
  if (offset > 0) {
    subroutines[offset - 1] = '\0';
  }

  printf("Usage: %s [options] <file>...\n"
         "Options:\n"
         "--tee            - print to file and to stdin (current: %d)\n"
         "--ignore-errors  - continue execution on errors (current: %d)\n"
         "-o <directory>   - output directory (current: %s)\n"
         "-s <subroutine>  - set or add subroutine for call graph generation "
         "(current: %s)\n",
         args->prog_name, args->tee, args->ignore_errors, args->output_dir,
         subroutines);
}

void parse_args(struct args_struct *args, int argc, char *argv[]) {
  if (argc > 0) {
    args->prog_name = argv[0];
  }

  struct option long_options[] = {
      {"tee", no_argument, &args->tee, 1},
      {"ignore-errors", no_argument, &args->ignore_errors, 1},
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
      if (long_options[option_index].flag != 0) {
        break;
      }
      break;
    case 'o':
      args->output_dir = optarg;
      break;
    case 's':
      args->subroutines->put(args->subroutines, optarg);
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
    args->input_files->put(args->input_files, (void *)file);
  }

  if (args->subroutines->size(args->subroutines) == 0) {
    args->subroutines->put(args->subroutines, "main");
  }
}

// filename is not directory
void ensure_dir_exist(const char *filename) {
  char *dir = dirname(filename);
  dir_create_p(dir);
  free(dir);
}

// ast - ext, dot - file extension
char *get_ast_dot_output(const struct args_struct *args,
                         const option2_ast *ast) {
  char *suffix = ".ast.dot";
  char *prefix =
      join_paths(args->output_dir, option2_ast_filename(ast));

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
  return args->subroutines->get(args->subroutines, subroutine) != NULL;
}

void print_exceptions(struct args_struct *_, const p_list exceptions) {
  for (list_iter it = exceptions->head; it != NULL; it = it->next) {
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

int execute(struct args_struct *args) {
  int error = 0;

  // stage: build ast + handle exceptions
  p_list asts = option2_ast_build_many(args->input_files);

  for (list_iter it = asts->head; it != NULL; it = it->next) {
    option2_ast *ast = it->data;
    if (ast->exceptions->size(ast->exceptions) != 0) {
      print_exceptions(args, ast->exceptions);
      error = 1;
    }
  }

  // stage: ast dot
  if (!error || args->ignore_errors) {
    for (list_iter it = asts->head; it != NULL; it = it->next) {
      option2_ast *ast = it->data;

      dot_string *dot = dot_ast(ast);
      char *output = get_ast_dot_output(args, ast);
      write_dot(args, dot, output);
      free(output);
      dot->free(dot);
    }
  }

  // stage: build cfg + handle exceptions
  ir_ctx *inter = NULL;

  if (!error || args->ignore_errors) {
    inter = option2_ir_build(asts);
    if (inter->exceptions->size(inter->exceptions) != 0) {
      print_exceptions(args, inter->exceptions);
      error = 1;
    }
  }

  // stage: cfg and cg dots
  if (!error || args->ignore_errors) {
    for (list_iter it = inter->subroutines->head; it != NULL; it = it->next) {
      ir_subroutine *sub = it->data;
      const char *sub_name = sub->signature->name;
      const char *source = (char *)sub->file_name;

      dot_string *dot_cfg = dot_inter_cfg(inter, sub);
      char *output_cfg = get_cfg_dot_output(args, source, sub_name);
      write_dot(args, dot_cfg, output_cfg);
      free(output_cfg);
      dot_cfg->free(dot_cfg);

      if (has_subroutine(args, sub_name)) {
        dot_string *dot_cg = dot_inter_cg(inter, sub);
        char *output_cg = get_cg_dot_output(args, source, sub_name);
        write_dot(args, dot_cg, output_cg);
        free(output_cg);
        dot_cg->free(dot_cg);
      }
    }
  }

  if (inter != NULL) {
    inter->free(inter);
  }

  asts->free(asts);

  return error && !args->ignore_errors;
}

int main(int argc, char *argv[]) {
  int code;

  struct args_struct *args = args_new();
  parse_args(args, argc, argv);

  if (args->help) {
    usage(args);
  }

  if (!args->help && args->input_files->size(args->input_files) == 0) {
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
