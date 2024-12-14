#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ptrace.h>
#include <sys/user.h>

void cmd_print(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  struct user_regs_struct regs;
  char                    buf[64];

  char *args = trim(strdup(rest));

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  if (!args || !*args) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "no args specified");
    goto cleanup;
  }

  if (context_get_target_state(ctx) == STATE_TARGET_NOT_RUNNING) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not running");
    goto cleanup;
  }

  if (args[0] == '$') {
    char *reg = args + 1;

    if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs) == -1) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_appendln(ctx, "failed to get regs");
      goto cleanup;
    }

    size_t offset = SIZE_MAX;

    if (!(strcmp(reg, "rax"))) {
      offset = offsetof(struct user_regs_struct, rax);
    } else if (!(strcmp(reg, "rbx"))) {
      offset = offsetof(struct user_regs_struct, rbx);
    } else if (!(strcmp(reg, "rcx"))) {
      offset = offsetof(struct user_regs_struct, rcx);
    } else if (!(strcmp(reg, "rdx"))) {
      offset = offsetof(struct user_regs_struct, rdx);
    } else if (!(strcmp(reg, "rsi"))) {
      offset = offsetof(struct user_regs_struct, rsi);
    } else if (!(strcmp(reg, "rdi"))) {
      offset = offsetof(struct user_regs_struct, rdi);
    } else if (!(strcmp(reg, "rbp"))) {
      offset = offsetof(struct user_regs_struct, rbp);
    } else if (!(strcmp(reg, "rsp"))) {
      offset = offsetof(struct user_regs_struct, rsp);
    } else if (!(strcmp(reg, "r8"))) {
      offset = offsetof(struct user_regs_struct, r8);
    } else if (!(strcmp(reg, "r9"))) {
      offset = offsetof(struct user_regs_struct, r9);
    } else if (!(strcmp(reg, "r10"))) {
      offset = offsetof(struct user_regs_struct, r10);
    } else if (!(strcmp(reg, "r11"))) {
      offset = offsetof(struct user_regs_struct, r11);
    } else if (!(strcmp(reg, "r12"))) {
      offset = offsetof(struct user_regs_struct, r12);
    } else if (!(strcmp(reg, "r13"))) {
      offset = offsetof(struct user_regs_struct, r13);
    } else if (!(strcmp(reg, "r14"))) {
      offset = offsetof(struct user_regs_struct, r14);
    } else if (!(strcmp(reg, "r15"))) {
      offset = offsetof(struct user_regs_struct, r15);
    } else if (!(strcmp(reg, "rip"))) {
      offset = offsetof(struct user_regs_struct, rip);
    } else if (!(strcmp(reg, "eflags"))) {
      offset = offsetof(struct user_regs_struct, eflags);
    } else if (!(strcmp(reg, "cs"))) {
      offset = offsetof(struct user_regs_struct, cs);
    } else if (!(strcmp(reg, "ss"))) {
      offset = offsetof(struct user_regs_struct, ss);
    } else if (!(strcmp(reg, "ds"))) {
      offset = offsetof(struct user_regs_struct, ds);
    } else if (!(strcmp(reg, "es"))) {
      offset = offsetof(struct user_regs_struct, es);
    } else if (!(strcmp(reg, "fs"))) {
      offset = offsetof(struct user_regs_struct, fs);
    } else if (!(strcmp(reg, "gs"))) {
      offset = offsetof(struct user_regs_struct, gs);
    }

    if (offset == SIZE_MAX) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_append(ctx, "unknown register ");
      context_err_appendln(ctx, reg);
      goto cleanup;
    }

    snprintf(buf, STRMAXLEN(buf), "%#llx",
             *(unsigned long long *)(((void *)&regs) + offset));
    context_out_appendln(ctx, buf);
  } else {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "unknown args ");
    context_err_appendln(ctx, args);
  }

cleanup:
  if (args) {
    free(args);
  }
}

const char *cmd_print_desc = "Print register";
