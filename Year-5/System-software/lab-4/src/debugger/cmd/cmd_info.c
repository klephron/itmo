#include "cmd.h"

#include "debugger/context/str.h"
#include "util/macro.h"
#include "util/string.h"

#include <bfd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/user.h>

static const char *desc = "Show things about program being debugged";

static const char *help = "Usage: <command> "
                          "(sections|symbols|target|registers|breakpoints|"
                          "info|lines|strs|files)";

static void ctx_align_d(context *ctx, uint64_t max_cnt, uint64_t cur_cnt) {
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

static char *ctx_debug_idx_str(context *ctx, uint64_t str_idx) {
  return ctx->debug.strs[str_idx];
}

static void fn_register_g(context *ctx, struct user_regs_struct *regs,
                          const char *reg, size_t offset) {
  char                buf[64];
  unsigned long long *addr = (void *)regs + offset;

  context_out_append(ctx, reg);

  context_out_append(ctx, "\t");
  snprintf(buf, STRMAXLEN(buf), "0x%-16llx", *addr);
  context_out_append(ctx, buf);

  context_out_append(ctx, "\t");
  snprintf(buf, STRMAXLEN(buf), "%-16lld", *addr);
  context_out_append(ctx, buf);

  context_out_appendln(ctx, "");
}

static void fn_sections(context *ctx) {
  if (cmd_error_if_no_target(ctx)) {
    return;
  }
  for (asection *section = ctx->abfd->sections; section;
       section = section->next) {
    char          buf[64];
    const char   *name = bfd_section_name(section);
    bfd_vma       address = bfd_section_vma(section);
    bfd_size_type size = bfd_section_size(section);
    context_out_append(ctx, "name: ");
    context_out_append(ctx, name);
    context_out_append(ctx, ",\t");
    context_out_append(ctx, "address: ");
    snprintf(buf, STRMAXLEN(buf), "%#lx", address);
    context_out_append(ctx, buf);
    context_out_append(ctx, ",\t");
    context_out_append(ctx, "size: ");
    snprintf(buf, STRMAXLEN(buf), "%#lx", size);
    context_out_append(ctx, buf);
    context_out_appendln(ctx, " bytes");
  }
}

static void fn_symbols(context *ctx) {
  if (cmd_error_if_no_target(ctx)) {
    return;
  }

  for (int64_t i = 0; i < ctx->abfd_asymbols_count; ++i) {
    char        buf[64];
    const char *name = bfd_asymbol_name(ctx->abfd_asymbols[i]);
    asection   *section = bfd_asymbol_section(ctx->abfd_asymbols[i]);
    const char *section_name = bfd_section_name(section);
    bfd_vma     address = bfd_asymbol_value(ctx->abfd_asymbols[i]);
    context_out_append(ctx, "section: ");
    context_out_append(ctx, section_name);
    context_out_append(ctx, ",\t");
    context_out_append(ctx, "name: ");
    context_out_append(ctx, name);
    context_out_append(ctx, ",\t");
    context_out_append(ctx, "address: ");
    snprintf(buf, STRMAXLEN(buf), "%#lx", address);
    context_out_appendln(ctx, buf);
  }
}

static void fn_target(context *ctx) {
  char buf[64];

  if (cmd_error_if_no_target(ctx)) {
    return;
  }

  context_out_append(ctx, "file: ");
  context_out_append(ctx, ctx->fname);
  context_out_append(ctx, ", status: ");
  context_out_append(ctx, state_target_enum_str(context_tg_get_state(ctx)));

  if (context_tg_get_state(ctx) != TARGET_NOT_RUNNING) {
    context_out_append(ctx, ", pid: ");
    snprintf(buf, STRMAXLEN(buf), "%d", ctx->pid);
    context_out_appendln(ctx, buf);
  } else {
    context_out_appendln(ctx, "");
  }
}

static void fn_registers(context *ctx) {
  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    return;
  }

  struct user_regs_struct regs;

  if (context_tg_get_state(ctx) != TARGET_NOT_RUNNING) {
    context_error(ctx, "failed to get regs");
    return;
  }

  fn_register_g(ctx, &regs, "rax", offsetof(struct user_regs_struct, rax));
  fn_register_g(ctx, &regs, "rbx", offsetof(struct user_regs_struct, rbx));
  fn_register_g(ctx, &regs, "rcx", offsetof(struct user_regs_struct, rcx));
  fn_register_g(ctx, &regs, "rdx", offsetof(struct user_regs_struct, rdx));
  fn_register_g(ctx, &regs, "rsi", offsetof(struct user_regs_struct, rsi));
  fn_register_g(ctx, &regs, "rdi", offsetof(struct user_regs_struct, rdi));
  fn_register_g(ctx, &regs, "rbp", offsetof(struct user_regs_struct, rbp));
  fn_register_g(ctx, &regs, "rsp", offsetof(struct user_regs_struct, rsp));
  fn_register_g(ctx, &regs, "r8", offsetof(struct user_regs_struct, r8));
  fn_register_g(ctx, &regs, "r9", offsetof(struct user_regs_struct, r9));
  fn_register_g(ctx, &regs, "r10", offsetof(struct user_regs_struct, r10));
  fn_register_g(ctx, &regs, "r11", offsetof(struct user_regs_struct, r11));
  fn_register_g(ctx, &regs, "r12", offsetof(struct user_regs_struct, r12));
  fn_register_g(ctx, &regs, "r13", offsetof(struct user_regs_struct, r13));
  fn_register_g(ctx, &regs, "r14", offsetof(struct user_regs_struct, r14));
  fn_register_g(ctx, &regs, "r15", offsetof(struct user_regs_struct, r15));
  fn_register_g(ctx, &regs, "rip", offsetof(struct user_regs_struct, rip));
  fn_register_g(ctx, &regs, "eflags",
                offsetof(struct user_regs_struct, eflags));
  fn_register_g(ctx, &regs, "cs", offsetof(struct user_regs_struct, cs));
  fn_register_g(ctx, &regs, "ss", offsetof(struct user_regs_struct, ss));
  fn_register_g(ctx, &regs, "ds", offsetof(struct user_regs_struct, ds));
  fn_register_g(ctx, &regs, "es", offsetof(struct user_regs_struct, es));
  fn_register_g(ctx, &regs, "fs", offsetof(struct user_regs_struct, fs));
  fn_register_g(ctx, &regs, "gs", offsetof(struct user_regs_struct, gs));
}

static void fn_breakpoints(context *ctx) {
  char   buf[64];
  size_t cnt = 0;
  for (hashset_breakpoint_it it = hashset_breakpoint_begin(ctx->breaks);
       !it.end(&it); it.next(&it)) {
    ++cnt;
  }

  breakpoint **breakpoints = MALLOCN(breakpoint *, cnt);
  size_t       i = 0;
  for (hashset_breakpoint_it it = hashset_breakpoint_begin(ctx->breaks);
       !it.end(&it); it.next(&it)) {
    breakpoints[i++] = it.get(&it);
  }

  if (cnt > 0) {
    for (size_t i = 0; i < cnt - 1; ++i) {
      for (size_t j = 0; j < cnt - i - 1; ++j) {
        if (breakpoints[j]->id > breakpoints[j + 1]->id) {
          breakpoint *tmp = breakpoints[j];
          breakpoints[j] = breakpoints[j + 1];
          breakpoints[j + 1] = tmp;
        }
      }
    }
  }

  for (size_t i = 0; i < cnt; ++i) {
    breakpoint *b = breakpoints[i];

    context_out_append(ctx, "num: ");
    snprintf(buf, STRMAXLEN(buf), "%zu", b->id);
    context_out_append(ctx, buf);

    context_out_append(ctx, ", address: ");
    snprintf(buf, STRMAXLEN(buf), "0x%016lx", b->address);
    context_out_append(ctx, buf);

    context_out_append(ctx, ", file: ");
    if (b->file) {
      context_out_append(ctx, b->file);
    } else {
      context_out_append(ctx, "-");
    }

    context_out_append(ctx, ", line: ");
    if (b->line != UINT64_MAX) {
      snprintf(buf, STRMAXLEN(buf), "%zu", b->line);
      context_out_append(ctx, buf);
    } else {
      context_out_append(ctx, "-");
    }

    context_out_append(ctx, ", enabled: ");
    if (b->is_enabled) {
      context_out_append(ctx, "true");
    } else {
      context_out_append(ctx, "false");
    }

    context_out_appendln(ctx, "");
  }

  free(breakpoints);
}

static void fn_debug_info(context *ctx) {
  if (cmd_error_if_no_target(ctx)) {
    return;
  }
  char buf[64];

  snprintf(buf, STRMAXLEN(buf), "subroutines_cnt: %zu",
           ctx->debug.subroutines_cnt);
  context_out_appendln(ctx, buf);

  snprintf(buf, STRMAXLEN(buf), "files_cnt: %zu", ctx->debug.files_cnt);
  context_out_appendln(ctx, buf);

  context_out_appendln(ctx, "subroutines: ");
  for (uint64_t i = 0; i < ctx->debug.subroutines_cnt; ++i) {
    debug_sub *sub = ctx->debug.subroutines[i];

    context_out_append(ctx, "- name: ");
    context_out_appendln(ctx, ctx_debug_idx_str(ctx, sub->strs_index));

    snprintf(buf, STRMAXLEN(buf), "\taddress_start: 0x%lx", sub->address_start);
    context_out_appendln(ctx, buf);

    snprintf(buf, STRMAXLEN(buf), "\taddress_end: 0x%lx", sub->address_end);
    context_out_appendln(ctx, buf);

    snprintf(buf, STRMAXLEN(buf), "\taddress_epilogue_start: 0x%lx",
             sub->address_epilogue_start);
    context_out_appendln(ctx, buf);

    snprintf(buf, STRMAXLEN(buf), "\taddress_prologue_end: 0x%lx",
             sub->address_prologue_end);
    context_out_appendln(ctx, buf);

    snprintf(buf, STRMAXLEN(buf), "\targs_cnt: %lu", sub->args_cnt);
    context_out_appendln(ctx, buf);

    for (uint64_t j = 0; j < sub->args_cnt; ++j) {
      debug_info_arg arg = sub->args[j];

      snprintf(buf, STRMAXLEN(buf), "\t- name: %s",
               ctx_debug_idx_str(ctx, arg.strs_index));
      context_out_append(ctx, buf);

      snprintf(buf, STRMAXLEN(buf), ", rbp_offset: %ld", arg.rbp_offset);
      context_out_appendln(ctx, buf);
    }

    snprintf(buf, STRMAXLEN(buf), "\tvars_cnt: %lu", sub->vars_cnt);
    context_out_appendln(ctx, buf);

    for (uint64_t j = 0; j < sub->vars_cnt; ++j) {
      debug_var var = sub->vars[j];

      snprintf(buf, STRMAXLEN(buf), "\t- name: %s",
               ctx_debug_idx_str(ctx, var.strs_index));
      context_out_append(ctx, buf);

      snprintf(buf, STRMAXLEN(buf), ", rbp_offset: %ld", var.rbp_offset);
      context_out_appendln(ctx, buf);
    }
  }

  context_out_appendln(ctx, "files: ");
  for (uint64_t i = 0; i < ctx->debug.files_cnt; ++i) {
    debug_file *file = ctx->debug.files[i];
    snprintf(buf, STRMAXLEN(buf), "- id: %lu", i);
    context_out_append(ctx, buf);
    context_out_append(ctx, ", name: ");
    context_out_appendln(ctx, ctx_debug_idx_str(ctx, file->strs_index));
  }
}

static void fn_debug_lines(context *ctx) {
  if (cmd_error_if_no_target(ctx)) {
    return;
  }
  char buf[64];
  for (uint64_t i = 0; i < ctx->debug.lines_cnt; ++i) {
    debug_line line = ctx->debug.lines[i];

    snprintf(buf, STRMAXLEN(buf), "%zu: ", i);
    context_out_append(ctx, buf);
    ctx_align_d(ctx, ctx->debug.lines_cnt, i);

    context_out_append(ctx, "address: ");
    snprintf(buf, STRMAXLEN(buf), "0x%016lx", line.address);
    context_out_append(ctx, buf);

    context_out_append(ctx, ", file_id: ");
    snprintf(buf, STRMAXLEN(buf), "%lu", line.file_id);
    context_out_append(ctx, buf);

    context_out_append(ctx, ", line: ");
    snprintf(buf, STRMAXLEN(buf), "%lu", line.line);
    context_out_append(ctx, buf);

    context_out_appendln(ctx, "");
  }
}

static void fn_debug_strs(context *ctx) {
  if (cmd_error_if_no_target(ctx)) {
    return;
  }
  char buf[64];
  for (uint64_t i = 0; i < ctx->debug.strs_cnt; ++i) {
    snprintf(buf, STRMAXLEN(buf), "%lu: ", i);
    context_out_append(ctx, buf);
    context_out_appendln(ctx, ctx_debug_idx_str(ctx, i));
  }
}

static void fn_debug_files(context *ctx) {
  if (cmd_error_if_no_target(ctx)) {
    return;
  }
  char buf[64];
  for (uint64_t i = 0; i < ctx->debug.files_cnt; ++i) {
    debug_file *file = ctx->debug.files[i];

    snprintf(buf, STRMAXLEN(buf), "// id: %lu", i);
    context_out_append(ctx, buf);

    context_out_append(ctx, ", name: ");
    uint64_t strs_index = file->strs_index;
    char    *str = ctx->debug.strs[strs_index];
    context_out_appendln(ctx, str);

    for (uint64_t j = 1; j < file->lines_cnt; ++j) {
      snprintf(buf, STRMAXLEN(buf), "%lu: ", j);
      context_out_append(ctx, buf);
      ctx_align_d(ctx, file->lines_cnt, j);
      context_out_append(ctx, file->lines[j]);
    }
  }
}

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *args;
  args = trim(strdup(rest));

  if (!strcmp(args, "sections")) {
    fn_sections(ctx);
  } else if (!strcmp(args, "symbols")) {
    fn_symbols(ctx);
  } else if (!strcmp(args, "target")) {
    fn_target(ctx);
  } else if (!strcmp(args, "registers")) {
    fn_registers(ctx);
  } else if (!strcmp(args, "breakpoints")) {
    fn_breakpoints(ctx);
  } else if (!strcmp(args, "info")) {
    fn_debug_info(ctx);
  } else if (!strcmp(args, "lines")) {
    fn_debug_lines(ctx);
  } else if (!strcmp(args, "strs")) {
    fn_debug_strs(ctx);
  } else if (!strcmp(args, "files")) {
    fn_debug_files(ctx);
  } else {
    cmd_error_format(ctx, args, help);
  }

  if (args) {
    free(args);
  }
}

cmd *cmd_info(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
