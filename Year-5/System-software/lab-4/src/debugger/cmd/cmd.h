#pragma once

#include "debugger/context/context.h"

typedef struct cmd_handler_struct cmd_handler;
typedef void cmd_fn(cmd_handler *handler, context *ctx, const char *rest);

typedef struct cmd_entry_struct {
  cmd_fn     *fn;
  const char *desc_ref;
  const char *cmd_ref;
} cmd_entry;

cmd_entry *cmd_entry_new(cmd_fn fn, const char *desc_ref, const char *cmd_ref);
void       cmd_entry_free(cmd_entry *self);

int cmd_error_if_no_target(context *ctx);

cmd_fn             cmd_continue;
extern const char *cmd_continue_desc;
cmd_fn             cmd_disassemble;
extern const char *cmd_disassemble_desc;
cmd_fn             cmd_echo;
extern const char *cmd_echo_desc;
cmd_fn             cmd_examine;
extern const char *cmd_examine_desc;
cmd_fn             cmd_exit;
extern const char *cmd_exit_desc;
cmd_fn             cmd_file;
extern const char *cmd_file_desc;
cmd_fn             cmd_help;
extern const char *cmd_help_desc;
cmd_fn             cmd_info;
extern const char *cmd_info_desc;
cmd_fn             cmd_kill;
extern const char *cmd_kill_desc;
cmd_fn             cmd_print;
extern const char *cmd_print_desc;
cmd_fn             cmd_run;
extern const char *cmd_run_desc;
