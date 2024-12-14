#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "debugger/cmd/cmd.h"
#include "debugger/cmd/handler.h"
#include "debugger/context/context.h"
#include "debugger/context/str.h"
#include "util/log.h"
#include "util/macro.h"

typedef struct args_struct {
  char *prog_name;
  // options
  char *init_file;
  // status
  int code;
  int help;
} args;

static args *args_new() {
  args *args = MALLOC(struct args_struct);

  args->prog_name = NULL;
  args->init_file = NULL;

  args->code = 0;
  args->help = 0;

  return args;
}

static void args_free(args *args) {
  if (args) {
    free(args);
  }
}

static void usage(args *args) {
  printf("Usage: %s [options] <file>...\n"
         "Options:\n"
         "-i <file>  - init file (current: %s)\n"
         "-h         - show help\n",
         args->prog_name, args->init_file);
}

static void parse(args *args, int argc, char *argv[]) {
  int           option_index;
  int           has_next;
  struct option long_options[] = {};

  if (argc > 0) {
    args->prog_name = argv[0];
  }

  option_index = 0;
  has_next = 1;

  while (has_next) {
    unsigned c;
    c = getopt_long(argc, argv, "i:h", long_options, &option_index);

    switch (c) {
    case EOF:
      has_next = 0;
      break;
    case 0:
      if (long_options[option_index].flag) {
        break;
      }
      break;
    case 'i':
      args->init_file = optarg;
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
}

static char *next_cmd(FILE *init_fp, FILE **fp_out) {
  char  *line = NULL;
  size_t len = 0;

  if (init_fp && getline(&line, &len, init_fp) != -1) {
    *fp_out = init_fp;
  } else {
    getline(&line, &len, stdin);
    *fp_out = stdin;
  }

  return line;
}

static void add(cmd_handler *h) {
  cmd_handler_register(h, cmd_entry_new(cmd_exit, cmd_exit_desc, "exit"));
  cmd_handler_register(h, cmd_entry_new(cmd_echo, cmd_echo_desc, "echo"));
  cmd_handler_register(h, cmd_entry_new(cmd_file, cmd_file_desc, "file"));
  cmd_handler_register(h, cmd_entry_new(cmd_file, cmd_file_desc, "f"));
  cmd_handler_register(h, cmd_entry_new(cmd_run, cmd_run_desc, "run"));
  cmd_handler_register(h, cmd_entry_new(cmd_run, cmd_run_desc, "r"));
  cmd_handler_register(h, cmd_entry_new(cmd_info, cmd_info_desc, "info"));
  cmd_handler_register(h, cmd_entry_new(cmd_info, cmd_info_desc, "i"));
  cmd_handler_register(h, cmd_entry_new(cmd_kill, cmd_kill_desc, "kill"));
  cmd_handler_register(h, cmd_entry_new(cmd_kill, cmd_kill_desc, "k"));
  cmd_handler_register(h, cmd_entry_new(cmd_print, cmd_print_desc, "print"));
  cmd_handler_register(h, cmd_entry_new(cmd_print, cmd_print_desc, "p"));
  cmd_handler_register(h,
                       cmd_entry_new(cmd_examine, cmd_examine_desc, "examine"));
  cmd_handler_register(h, cmd_entry_new(cmd_examine, cmd_examine_desc, "x"));
  cmd_handler_register(
      h, cmd_entry_new(cmd_disassemble, cmd_disassemble_desc, "disassemble"));
  cmd_handler_register(
      h, cmd_entry_new(cmd_disassemble, cmd_disassemble_desc, "dis"));
  cmd_handler_register(
      h, cmd_entry_new(cmd_continue, cmd_continue_desc, "continue"));
  cmd_handler_register(h, cmd_entry_new(cmd_continue, cmd_continue_desc, "c"));
  cmd_handler_register(h, cmd_entry_new(cmd_help, cmd_help_desc, "help"));
  cmd_handler_register(h, cmd_entry_new(cmd_help, cmd_help_desc, "h"));
}

static int execute(args *args) {
  FILE *init_fp = NULL;

  if (args->init_file) {
    init_fp = fopen(args->init_file, "r");
    if (!init_fp) {
      warn("init file %s doesn't exist", args->init_file);
    }
  }

  context *ctx = context_new();

  cmd_handler *handler = cmd_handler_new();
  add(handler);

  while (ctx->s_debugger != STATE_DEBUGGER_EXITING) {
    FILE *fp = NULL;

    fprintf(stdout, "(dbg) ");

    char *cmd = next_cmd(init_fp, &fp);
    if (fileno(fp) != STDIN_FILENO) {
      fprintf(stdout, "%s", cmd);
    }

    if (cmd) {
      cmd_handler_handle(handler, ctx, cmd);
      free(cmd);
    }

    debug("debugger: %s, target: %s", state_debugger_enum_str(ctx->s_debugger),
          state_target_enum_str(context_get_target_state(ctx)));

    if (string_buffer_size(ctx->buf_out)) {
      fprintf(stdout, "%s", string_buffer_data(ctx->buf_out));
      string_buffer_reset(ctx->buf_out);
    }

    if (ctx->s_debugger == STATE_DEBUGGER_ERROR) {
      fprintf(stderr, "error: %s", string_buffer_data(ctx->buf_err));
      string_buffer_reset(ctx->buf_err);
      ctx->s_debugger = STATE_DEBUGGER_IDLE;
    }
  }

  cmd_handler_free(handler);
  context_free(ctx);

  if (init_fp) {
    fclose(init_fp);
  }

  return args->code;
}

int main(int argc, char *argv[]) {
  int   code;
  args *args = args_new();
  parse(args, argc, argv);

  if (args->help) {
    usage(args);
  }

  if (!args->help && args->code == 0) {
    args->code = execute(args);
  }

  code = args->code;

  args_free(args);

  return code;
}
