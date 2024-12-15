#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <stdlib.h>

static const char *desc = "Disassemble memory";

static const char *help = "Usage: <command> <start> <size>\n"
                          "\t<start>: start address in hex\n"
                          "\t<size>: b(byte)|h(halfword)|w(word)|g(giant)";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *args = trim(strdup(rest));
  char *arg_address = NULL;
  char *arg_size = NULL;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  {
    char *token_args = strdup(args);
    char *token = strtok(token_args, " ");
    if (token) {
      arg_address = strdup(token);
      token = strtok(NULL, " ");
    }
    if (token) {
      arg_size = strdup(token);
      token = strtok(NULL, " ");
    }
  }

  if (!arg_size || !arg_address) {
    goto error_format;
  }

  uint64_t address;
  sscanf(arg_address, "%lx", &address);

  uint64_t size;
  sscanf(arg_size, "%lu", &size);

  cmd_disassemble_at_address(ctx, address, size);
  goto cleanup;

error_format:
  cmd_error_format(ctx, args, help);

cleanup:
  if (args) {
    free(args);
  }
  if (arg_size) {
    free(arg_size);
  }
  if (arg_address) {
    free(arg_address);
  }
  return;
}

static int fn_fprintf(void *stream, const char *fmt, ...) {
  char buf[256];

  string_buffer *buffer = (string_buffer *)stream;

  va_list args;
  va_start(args, fmt);

  int len = vsnprintf(buf, STRMAXLEN(buf), fmt, args);
  va_end(args);

  string_buffer_append(buffer, buf);
  return len;
}

static int fn_fprintf_styled_ftype(void *stream, enum disassembler_style style,
                                   const char *fmt, ...) {
  UNUSED(style);
  char           buf[256];
  string_buffer *buffer = (string_buffer *)stream;

  va_list args;
  va_start(args, fmt);

  int len = vsnprintf(buf, STRMAXLEN(buf), fmt, args);
  va_end(args);

  string_buffer_append(buffer, buf);
  return len;
}

cmd *cmd_disassemble(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}

void cmd_disassemble_at_address(context *ctx, uint64_t address, uint64_t size) {
  char           buf[64];
  string_buffer *buffer = string_buffer_new(0, 0);

  struct disassemble_info info;
  init_disassemble_info(&info, buffer, fn_fprintf, fn_fprintf_styled_ftype);

  info.arch = bfd_arch_i386;
  info.mach = bfd_mach_x86_64;

  info.application_data = ctx;
  info.read_memory_func = cmd_read_memory;
  info.memory_error_func = cmd_memory_error;

  disassembler_ftype disasm =
      disassembler(bfd_arch_i386, false, bfd_mach_x86_64, NULL);

  bfd_vma pc = address;
  while (pc < address + size) {
    snprintf(buf, STRMAXLEN(buf), "0x%016lx\t", pc);
    string_buffer_append(buffer, buf);

    size_t insn_size = disasm(pc, &info);
    pc += insn_size;
    if (pc < address + size) {
      string_buffer_append(buffer, "\n");
    }
  }

  char *data = string_buffer_detach(buffer);
  if (data) {
    context_out_appendln(ctx, data);
    free(data);
  }
}
