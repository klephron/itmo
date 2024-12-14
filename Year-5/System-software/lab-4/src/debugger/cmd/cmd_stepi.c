#include "debugger/cmd/cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"
#include "util/string.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/user.h>
#include <sys/wait.h>

static const char *desc = "Step one instruction exactly";

static const char *help = "Usage: <command> [<n>]\n"
                          "\t<n>: number of times";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char                   *args = NULL;
  size_t                  n = 1;
  struct user_regs_struct regs;

  args = trim(strdup(rest));

  if (args && *args) {
    sscanf(args, "%lu", &n);
  }

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  for (size_t i = 0; i < n; ++i) {
    if (context_get_target_state(ctx) == TARGET_NOT_RUNNING) {
      ctx->s_debugger = DEBUGGER_ERROR;
      context_err_appendln(ctx, "target is not running");
      goto cleanup;
    }

    if (ptrace(PTRACE_SINGLESTEP, ctx->pid, NULL, NULL) == -1) {
      ctx->s_debugger = DEBUGGER_ERROR;
      context_err_appendln(ctx, "ptrace error");
      goto cleanup;
    }

    int status;
    if (waitpid(ctx->pid, &status, 0) == -1) {
      ctx->s_debugger = DEBUGGER_ERROR;
      context_err_appendln(ctx, "waitpid error");
      goto cleanup;
    }
  }

  if (context_get_target_state(ctx) == TARGET_RUNNING) {
    if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs) == -1) {
      ctx->s_debugger = DEBUGGER_ERROR;
      context_err_appendln(ctx, "failed to get regs");
      goto cleanup;
    }

    cmd_disassemble_at_address(ctx, regs.rip, 1);
  }

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_stepi(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
