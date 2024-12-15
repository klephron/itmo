#include "cmd.h"

#include "debugger/context/str.h"
#include "util/macro.h"
#include "util/string.h"

#include <bfd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/user.h>

static const char *desc = "Info sections, symbols, target, register";

static const char *help =
    "Usage: <command> (sections|symbols|target|registers)";

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

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *args;

  args = trim(strdup(rest));

  if (!strcmp(args, "sections")) {
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

  } else if (!strcmp(args, "symbols")) {
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
  } else if (!strcmp(args, "target")) {
    char buf[64];

    if (cmd_error_if_no_target(ctx)) {
      goto cleanup;
    }

    context_out_append(ctx, "file: ");
    context_out_append(ctx, ctx->fname);
    context_out_append(ctx, ", status: ");
    context_out_append(ctx,
                       state_target_enum_str(context_get_target_state(ctx)));

    if (context_get_target_state(ctx) != TARGET_NOT_RUNNING) {
      context_out_append(ctx, ", pid: ");
      snprintf(buf, STRMAXLEN(buf), "%d", ctx->pid);
      context_out_appendln(ctx, buf);
    } else {
      context_out_appendln(ctx, "");
    }
  } else if (!strcmp(args, "registers")) {
    if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
      goto cleanup;
    }

    struct user_regs_struct regs;

    if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs) == -1) {
      cmd_error(ctx, "failed to get regs");
      goto cleanup;
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
  } else {
    goto error_format;
  }
  goto cleanup;

error_format:
  cmd_error_format(ctx, args, help);

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_info(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
