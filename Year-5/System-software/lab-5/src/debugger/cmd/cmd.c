#include "cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"

#include <stdlib.h>
#include <sys/ptrace.h>

cmd *cmd_entry_new(cmd_fn fn, const char *desc_ref, const char *help_ref,
                   const char *cmd_ref) {
  cmd *self = MALLOC(cmd);
  self->fn = fn;
  self->desc_ref = desc_ref;
  self->help_ref = help_ref;
  self->cmd_ref = cmd_ref;
  return self;
}

void cmd_entry_free(cmd *self) {
  if (self) {
    free(self);
  }
}

int cmd_error_if_no_target(context *ctx) {
  if (context_tg_get_state(ctx) == TARGET_NO_TARGET) {
    context_error(ctx, "target is not loaded");
    return 1;
  }
  return 0;
}

int cmd_error_if_not_running(context *ctx) {
  if (context_tg_get_state(ctx) == TARGET_NOT_RUNNING) {
    context_error(ctx, "target is not running");
    return 1;
  }
  return 0;
}

void cmd_error_format(context *ctx, const char *args, const char *help) {
  context_err_append(ctx, "unexpected args ");
  context_err_appendln(ctx, args);
  context_error(ctx, help);
}

void cmd_ctx_memory_error(int status, bfd_vma memaddr,
                          struct disassemble_info *info) {
  UNUSED(status);
  UNUSED(memaddr);
  UNUSED(info);
}

int cmd_ctx_read_memory(bfd_vma memaddr, bfd_byte *addr, unsigned int length,
                        struct disassemble_info *info) {
  context *ctx = info->application_data;
  uint64_t data;

  uint64_t bytes = 0;
  for (bfd_vma addr_cur = memaddr; bytes < length; addr_cur += sizeof(long)) {
    if (context_tg_peekdata(ctx, addr_cur, &data) == TARGET_ERROR) {
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

void cmd_ctx_align(context *ctx, uint64_t max_cnt, uint64_t cur_cnt) {
  if (cur_cnt == 0) {
    cur_cnt = 1;
  }
  while (max_cnt && cur_cnt) {
    max_cnt /= 10;
    cur_cnt /= 10;
  }
  while (max_cnt) {
    context_out_append(ctx, " ");
    max_cnt /= 10;
  }
}

void cmd_ctx_out_file_header(context *ctx, uint64_t file_id) {
  char buf[64];
  snprintf(buf, STRMAXLEN(buf), "// id: %zu", file_id);
  context_out_append(ctx, buf);
  context_out_append(ctx, ", name: ");
  context_out_appendln(ctx, context_dbg_file_by_id(ctx, file_id));
}

void cmd_ctx_out_source_line(context *ctx, debug_line line) {
  char buf[64];

  debug_file *file = ctx->debug.files[line.file_id];
  cmd_ctx_out_file_header(ctx, line.file_id);

  cmd_ctx_align(ctx, ctx->debug.lines_cnt, line.line);
  snprintf(buf, STRMAXLEN(buf), "%zu│", line.line);

  context_out_append(ctx, buf);
  context_out_append(ctx, file->lines[line.line]);
}

void cmd_ctx_out_line(context *ctx, debug_line line) {
  if (ctx->options.source_lang && line.file_id != UINT64_MAX &&
      line.line != UINT64_MAX) {
    cmd_ctx_out_source_line(ctx, line);
  } else {
    cmd_disassemble_at_address(ctx, line.address, 1);
  }
}
