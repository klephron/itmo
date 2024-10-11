#include <getopt.h>
#include <stdio.h>

#include "util/list.h"
#include "util/list_funcs.h"
#include "util/macro.h"

#include "dot.h"
#include "exception.h"
#include "option2.h"

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

int execute(struct args_struct *args) {
  int error = 0;

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
