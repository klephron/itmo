#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <dis-asm.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>

static int cmd_disassemble_fprintf(void *stream, const char *fmt, ...) {
  char buf[256];

  string_buffer *buffer = (string_buffer *)stream;

  va_list args;
  va_start(args, fmt);

  int len = vsnprintf(buf, STRMAXLEN(buf), fmt, args);
  va_end(args);

  string_buffer_append(buffer, buf);
  return len;
}

static int cmd_disassemble_fprintf_styled_ftype(void                   *stream,
                                                enum disassembler_style style,
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

static int cmd_disassemble_read_memory(bfd_vma memaddr, bfd_byte *addr,
                                       unsigned int             length,
                                       struct disassemble_info *info) {
  context *ctx = (context *)info->application_data;

  uint64_t bytes = 0;
  for (bfd_vma addr_cur = memaddr; bytes < length; addr_cur += sizeof(long)) {
    long data = ptrace(PTRACE_PEEKDATA, ctx->pid, addr_cur, NULL);
    if (errno) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_append(ctx, "ptrace error while fetching data: ");
      context_err_appendln(ctx, strerror(errno));
      return 1;
    }
    uint64_t bytes_to_copy = sizeof(long);
    if (bytes + bytes_to_copy > length) {
      bytes_to_copy = length - bytes;
    }

    memcpy(addr + bytes, &data, bytes_to_copy);
    bytes += bytes_to_copy;
  }
  return 0;
}

static void cmd_disassemble_memory_error(int status, bfd_vma memaddr,
                                         struct disassemble_info *info) {
  char buf[256];

  snprintf(buf, STRMAXLEN(buf), "memory error at address 0x%lx, status: %d\n",
           (long)memaddr, status);
  context_err_appendln((context *)info->application_data, buf);
}

void cmd_disassemble(context *ctx, const char *rest) {
  char *args = trim(strdup(rest));
  char *arg_address = NULL;
  char *arg_size = NULL;

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  if (context_get_target_state(ctx) == STATE_TARGET_NOT_RUNNING) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not running");
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
    goto err_format;
  }

  uint64_t address;
  sscanf(arg_address, "%lx", &address);

  uint64_t size;
  sscanf(arg_size, "%lu", &size);

  string_buffer *buffer = string_buffer_new(0, 0);

  struct disassemble_info info;
  init_disassemble_info(&info, buffer, cmd_disassemble_fprintf,
                        cmd_disassemble_fprintf_styled_ftype);

  info.arch = bfd_arch_i386;
  info.mach = bfd_mach_x86_64;

  info.read_memory_func = cmd_disassemble_read_memory;
  info.memory_error_func = cmd_disassemble_memory_error;

  info.application_data = ctx;
  disassembler_ftype disasm =
      disassembler(bfd_arch_i386, false, bfd_mach_x86_64, NULL);

  bfd_vma pc = address;
  while (pc < address + size) {
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

  return;

err_format:
  ctx->s_debugger = STATE_DEBUGGER_ERROR;
  context_err_append(ctx, "unexpected args ");
  context_err_appendln(ctx, args);
  context_err_appendln(ctx, "Arguments: <start> <size>");
  context_err_appendln(ctx, "\t<start>: start address in hex");
  context_err_appendln(ctx, "\t<size>: b(byte)|h(halfword)|w(word)|g(giant)");
  goto cleanup;

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
