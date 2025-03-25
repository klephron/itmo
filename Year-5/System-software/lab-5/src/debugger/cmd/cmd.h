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
void cmd_error_format(context *ctx, const char *args, const char *help);

void cmd_ctx_memory_error(int status, bfd_vma memaddr,
                          struct disassemble_info *info);
int  cmd_ctx_read_memory(bfd_vma memaddr, bfd_byte *addr, unsigned int length,
                         struct disassemble_info *info);
void cmd_ctx_align(context *ctx, uint64_t max_cnt, uint64_t cur_cnt);
void cmd_ctx_out_source_line(context *ctx, debug_line line);
void cmd_ctx_out_line(context *ctx, debug_line line);
void cmd_ctx_out_file_header(context *ctx, uint64_t file_id);

// commands
typedef cmd *cmd_new(const char *cmd_ref);

cmd_new cmd_break;
cmd_new cmd_config;
cmd_new cmd_continue;
cmd_new cmd_delete;
cmd_new cmd_disassemble;
cmd_new cmd_echo;
cmd_new cmd_examine;
cmd_new cmd_exit;
cmd_new cmd_file;
cmd_new cmd_help;
cmd_new cmd_info;
cmd_new cmd_kill;
cmd_new cmd_next;
cmd_new cmd_nexti;
cmd_new cmd_print;
cmd_new cmd_run;
cmd_new cmd_step;
cmd_new cmd_stepi;
cmd_new cmd_where;

void cmd_disassemble_at_address(context *ctx, uint64_t address, uint64_t size);
void cmd_nexti_disasm_init(context *ctx, disassemble_info *info_p,
                           disassembler_ftype *dftype_p, int *is_call_p);
int  cmd_nexti_exec(context *ctx, disassemble_info *info_p,
                    disassembler_ftype *dftype_p, int *is_call_p, uint64_t n);
int  cmd_stepi_exec(context *ctx, uint64_t n);
