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
  char *args        = trim(strdup(rest));
  char *arg_address = NULL;
  char *arg_size    = NULL;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  {
    char *token_args = strdup(args);
    char *token      = strtok(token_args, " ");
    if (token) {
      arg_address = strdup(token);
      token       = strtok(NULL, " ");
    }
    if (token) {
      arg_size = strdup(token);
      token    = strtok(NULL, " ");
    }
    free(token_args);
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

typedef struct dis_ctx_struct {
  context *ctx;
  strbuf  *buffer;
  int      error;
} dis_ctx;

static int fn_fprintf(void *stream, const char *fmt, ...) {
  char buf[256];

  strbuf *buffer = (strbuf *)stream;

  va_list args;
  va_start(args, fmt);

  int len = vsnprintf(buf, STRMAXLEN(buf), fmt, args);
  va_end(args);

  strbuf_append(buffer, buf);
  return len;
}

static int fn_fprintf_styled_ftype(void *stream, enum disassembler_style style,
                                   const char *fmt, ...) {
  UNUSED(style);
  char    buf[256];
  strbuf *buffer = (strbuf *)stream;

  va_list args;
  va_start(args, fmt);

  int len = vsnprintf(buf, STRMAXLEN(buf), fmt, args);
  va_end(args);

  strbuf_append(buffer, buf);
  return len;
}

static void fn_memory_error(int status, bfd_vma memaddr,
                            struct disassemble_info *info) {
  UNUSED(status);
  UNUSED(memaddr);

  char     buf[64];
  dis_ctx *dis = info->application_data;
  dis->error   = 1;

  snprintf(buf, STRMAXLEN(buf), "[unable to peek data]");
  strbuf_append(dis->buffer, buf);
}

static int fn_read_memory(bfd_vma memaddr, bfd_byte *addr, unsigned int length,
                          struct disassemble_info *info) {
  dis_ctx *dis = info->application_data;
  uint64_t data;

  uint64_t bytes = 0;
  for (bfd_vma addr_cur = memaddr; bytes < length; addr_cur += sizeof(long)) {
    if (context_tg_peekdata(dis->ctx, addr_cur, &data) == TARGET_ERROR) {
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

cmd *cmd_disassemble(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}

void cmd_disassemble_at_address(context *ctx, uint64_t address, uint64_t size) {
  char    buf[64];
  strbuf *buffer = strbuf_new(0, 0);

  struct disassemble_info info;
  init_disassemble_info(&info, buffer, fn_fprintf, fn_fprintf_styled_ftype);

  info.arch = bfd_arch_i386;
  info.mach = bfd_mach_x86_64;

  dis_ctx dis            = (dis_ctx){.ctx = ctx, .buffer = buffer, .error = 0};
  info.application_data  = &dis;
  info.read_memory_func  = fn_read_memory;
  info.memory_error_func = fn_memory_error;

  disassembler_ftype disasm =
      disassembler(bfd_arch_i386, false, bfd_mach_x86_64, NULL);

  bfd_vma pc = address;
  while (pc < address + size) {
    snprintf(buf, STRMAXLEN(buf), "0x%016lx: \t", pc);
    strbuf_append(buffer, buf);

    size_t insn_size = disasm(pc, &info);
    pc += insn_size;
    context_out_appendln(ctx, strbuf_data(buffer));
    strbuf_reset(buffer);
  }
  if (dis.error) {
    context_error(ctx, "error occured while disassembling");
  }
  strbuf_free(buffer);
}
