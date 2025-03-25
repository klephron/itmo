#include "cmd.h"

#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ptrace.h>
#include <sys/user.h>

static const char *DESC = "Print register, argument or variable";

static const char *HELP = "Usage: <command> (<register>|<arg>|<var>)\n"
                          "\t<register>: $<name>\n"
                          "\t<arg>: argument name\n"
                          "\t<var>: variable name";

static int cmd_print_register(ctx *ctx, const char *rest) {
  char                    buf[64];
  struct user_regs_struct regs;
  const char             *reg = rest + 1;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    return -1;
  }

  if (ctx_tg_getregs(ctx, &regs) == TARGET_ERROR) {
    ctx_error(ctx, "failed to get regs");
    return -1;
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
    ctx->s_debugger = DEBUGGER_ERROR;
    ctx_err_append(ctx, "unknown register ");
    ctx_err_appendln(ctx, reg);
    return -1;
  }

  snprintf(buf, STRMAXLEN(buf), "%#llx",
           *(unsigned long long *)(((void *)&regs) + offset));
  ctx_out_appendln(ctx, buf);
  return 0;
}

static int cmd_print_param_var(ctx *ctx, const char *rest) {
  const char             *args = rest;
  struct user_regs_struct regs;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    return -1;
  }

  if (ctx_tg_getregs(ctx, &regs) == TARGET_ERROR) {
    ctx_error(ctx, "failed to get regs");
    return -1;
  }

  // get current subroutine based on rsp
  ctx_debug_sub *sub = ctx_dbg_sub_by_address(ctx, regs.rip);
  if (!sub) {
    ctx_error(ctx, "no debug info about current subroutine");
    return -1;
  }

  int found = 0;

  for (uint64_t i = 0; i < sub->params_cnt; ++i) {
    const ctx_debug_param *param = sub->params + i;
    const char *param_name       = ctx_dbg_str_by_id(ctx, param->debug_str_id);
    if (!strcmp(param_name, args)) {
      uint64_t address = regs.rbp + param->rbp_offset;
      UNUSED(address);

      warn("TODO: print param '%s' value: type", param_name);
      found = 1;
    }
  }

  for (uint64_t i = 0; i < sub->vars_cnt; ++i) {
    const ctx_debug_var *var      = sub->vars + i;
    const char          *var_name = ctx_dbg_str_by_id(ctx, var->debug_str_id);
    if (!strcmp(var_name, args)) {
      uint64_t address = regs.rbp + var->rbp_offset;
      UNUSED(address);

      warn("TODO: print var '%s' value: type", var_name);
      found = 1;
    }
  }

  if (!found) {
    ctx_err_append(ctx, "in subroutine ");
    ctx_err_append(ctx, ctx_dbg_str_by_id(ctx, sub->debug_str_id));
    ctx_error(ctx, " arg or var not found");
    return -1;
  }

  return 0;
}

static void cmd_print_fn(cmd_handler *handler, ctx *ctx, const char *rest) {
  UNUSED(handler);

  char *args = trim(strdup(rest));

  if (!args || !*args) {
    goto error_format;
  }

  if (args[0] == '$') {
    cmd_print_register(ctx, args);
  } else {
    cmd_print_param_var(ctx, args);
  }
  goto cleanup;

error_format:
  cmd_error_format(ctx, args, HELP);

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_print(const char *cmd_ref) {
  return cmd_entry_new(cmd_print_fn, DESC, HELP, cmd_ref);
}
