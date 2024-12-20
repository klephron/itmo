#include "debugger/cmd/cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"
#include "util/string.h"

#include <dis-asm.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/user.h>
#include <sys/wait.h>

static const char *desc =
    "Step one source line, but proceed through subroutine calls";

static const char *help = "Usage: <command> [<n>]\n"
                          "\t<n>: number of times";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char                   *args = NULL;
  size_t                  n = 1;
  struct user_regs_struct regs;
  debug_line              curr;

  args = trim(strdup(rest));

  if (args && *args) {
    sscanf(args, "%lu", &n);
  }

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  // get current address
  if (context_tg_getregs(ctx, &regs) == TARGET_ERROR) {
    context_error(ctx, "failed to get regs");
    goto cleanup;
  }

  // fix current address if on breakpoint
  if (context_tg_get_state(ctx) == TARGET_BRKPT) {
    context_tg_brkpt_fix_regs(&regs);
  }

  curr = ctx->prev_line;

  disassemble_info   info;
  disassembler_ftype dftype;
  int                is_call;
  cmd_nexti_disasm_init(ctx, &info, &dftype, &is_call);

  for (size_t i = 0; i < n; ++i) {
    while (!context_tg_prev_line_changed(ctx, curr)) {
      if (cmd_nexti_fn(ctx, &info, &dftype, &is_call, n) == -1) {
        goto cleanup;
      }

      if (!context_tg_is_running(ctx)) {
        break;
      }

      if (context_tg_getregs(ctx, &regs) == TARGET_ERROR) {
        context_error(ctx, "failed to get regs");
        goto cleanup;
      }

      if (context_tg_get_state(ctx) == TARGET_BRKPT) {
        context_tg_brkpt_fix_regs(&regs);
      }

      curr.address = regs.rip;
      context_dbg_line_by_address(ctx, &curr);
    }
    context_tg_prev_line_set(ctx, curr);
  }

  if (context_tg_is_running(ctx)) {
    cmd_ctx_out_line(ctx, curr);
  }

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_next(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
