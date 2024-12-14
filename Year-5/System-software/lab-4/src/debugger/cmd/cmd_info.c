#include "cmd.h"

#include "debugger/context/str.h"
#include "util/macro.h"
#include "util/string.h"

#include <bfd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/user.h>

static void cmd_info_register_g(context *ctx, struct user_regs_struct *regs,
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

void cmd_info(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *arg;

  arg = trim(strdup(rest));

  if (!strcmp(arg, "sections")) {
    if (cmd_error_if_no_target(ctx)) {
      goto cleanup;
    }

    asection *section = ctx->abfd->sections;
    while (section) {
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
      section = section->next;
    }

  } else if (!strcmp(arg, "symbols")) {
    if (cmd_error_if_no_target(ctx)) {
      goto cleanup;
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
  } else if (!strcmp(arg, "target")) {
    if (cmd_error_if_no_target(ctx)) {
      goto cleanup;
    }
    char buf[64];
    context_out_append(ctx, "file: ");
    context_out_append(ctx, ctx->fname);
    context_out_append(ctx, ", status: ");
    context_out_append(ctx,
                       state_target_enum_str(context_get_target_state(ctx)));
    if (context_get_target_state(ctx) != STATE_TARGET_NOT_RUNNING) {
      context_out_append(ctx, ", pid: ");
      snprintf(buf, STRMAXLEN(buf), "%d", ctx->pid);
      context_out_appendln(ctx, buf);
    } else {
      context_out_appendln(ctx, "");
    }
  } else if (!strcmp(arg, "registers")) {
    if (cmd_error_if_no_target(ctx)) {
      goto cleanup;
    }
    if (context_get_target_state(ctx) == STATE_TARGET_NOT_RUNNING) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_appendln(ctx, "target is not running");
      goto cleanup;
    }

    struct user_regs_struct regs;

    if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs) == -1) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_appendln(ctx, "failed to get regs");
      goto cleanup;
    }

    cmd_info_register_g(ctx, &regs, "rax",
                        offsetof(struct user_regs_struct, rax));
    cmd_info_register_g(ctx, &regs, "rbx",
                        offsetof(struct user_regs_struct, rbx));
    cmd_info_register_g(ctx, &regs, "rcx",
                        offsetof(struct user_regs_struct, rcx));
    cmd_info_register_g(ctx, &regs, "rdx",
                        offsetof(struct user_regs_struct, rdx));
    cmd_info_register_g(ctx, &regs, "rsi",
                        offsetof(struct user_regs_struct, rsi));
    cmd_info_register_g(ctx, &regs, "rdi",
                        offsetof(struct user_regs_struct, rdi));
    cmd_info_register_g(ctx, &regs, "rbp",
                        offsetof(struct user_regs_struct, rbp));
    cmd_info_register_g(ctx, &regs, "rsp",
                        offsetof(struct user_regs_struct, rsp));
    cmd_info_register_g(ctx, &regs, "r8",
                        offsetof(struct user_regs_struct, r8));
    cmd_info_register_g(ctx, &regs, "r9",
                        offsetof(struct user_regs_struct, r9));
    cmd_info_register_g(ctx, &regs, "r10",
                        offsetof(struct user_regs_struct, r10));
    cmd_info_register_g(ctx, &regs, "r11",
                        offsetof(struct user_regs_struct, r11));
    cmd_info_register_g(ctx, &regs, "r12",
                        offsetof(struct user_regs_struct, r12));
    cmd_info_register_g(ctx, &regs, "r13",
                        offsetof(struct user_regs_struct, r13));
    cmd_info_register_g(ctx, &regs, "r14",
                        offsetof(struct user_regs_struct, r14));
    cmd_info_register_g(ctx, &regs, "r15",
                        offsetof(struct user_regs_struct, r15));
    cmd_info_register_g(ctx, &regs, "rip",
                        offsetof(struct user_regs_struct, rip));
    cmd_info_register_g(ctx, &regs, "eflags",
                        offsetof(struct user_regs_struct, eflags));
    cmd_info_register_g(ctx, &regs, "cs",
                        offsetof(struct user_regs_struct, cs));
    cmd_info_register_g(ctx, &regs, "ss",
                        offsetof(struct user_regs_struct, ss));
    cmd_info_register_g(ctx, &regs, "ds",
                        offsetof(struct user_regs_struct, ds));
    cmd_info_register_g(ctx, &regs, "es",
                        offsetof(struct user_regs_struct, es));
    cmd_info_register_g(ctx, &regs, "fs",
                        offsetof(struct user_regs_struct, fs));
    cmd_info_register_g(ctx, &regs, "gs",
                        offsetof(struct user_regs_struct, gs));

  } else {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "unknown args ");
    context_err_appendln(ctx, arg);
    goto cleanup;
  }

cleanup:
  if (arg) {
    free(arg);
  }
}

const char *cmd_info_desc = "Info registers, sections or symbols";
