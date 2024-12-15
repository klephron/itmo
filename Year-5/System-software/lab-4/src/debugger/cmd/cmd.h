#pragma once

#include "debugger/context/context.h"
#include <dis-asm.h>

typedef struct cmd_handler_struct cmd_handler;
typedef void cmd_fn(cmd_handler *handler, context *ctx, const char *rest);

typedef struct cmd_struct {
  cmd_fn     *fn;
  const char *desc_ref;
  const char *cmd_ref;
  const char *help_ref;
} cmd;

cmd *cmd_entry_new(cmd_fn fn, const char *desc_ref, const char *help_ref,
                   const char *cmd_ref);
void cmd_entry_free(cmd *self);

// utility
int  cmd_error_if_no_target(context *ctx);
int  cmd_error_if_not_running(context *ctx);
void cmd_error(context *ctx, const char *msg);

void cmd_memory_error(int status, bfd_vma memaddr,
                      struct disassemble_info *info);
int  cmd_read_memory(bfd_vma memaddr, bfd_byte *addr, unsigned int length,
                     struct disassemble_info *info);

// commands
typedef cmd *cmd_new(const char *cmd_ref);

cmd_new cmd_continue;
cmd_new cmd_disassemble;
cmd_new cmd_echo;
cmd_new cmd_examine;
cmd_new cmd_exit;
cmd_new cmd_file;
cmd_new cmd_help;
cmd_new cmd_info;
cmd_new cmd_kill;
cmd_new cmd_nexti;
cmd_new cmd_print;
cmd_new cmd_run;
cmd_new cmd_stepi;

void cmd_disassemble_at_address(context *ctx, uint64_t address, uint64_t size);
