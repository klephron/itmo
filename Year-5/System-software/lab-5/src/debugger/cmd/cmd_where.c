#include "cmd.h"
#include "util/macro.h"

static const char *DESC = "Print stacktrace";

static const char *HELP = "Usage: <command>";

static uint64_t cmd_where_p_rip(uint64_t rbp) { return rbp + 8; }
static uint64_t cmd_where_p_rbp(uint64_t rbp) { return rbp; }

// TODO: check current instruction if in prologue/epilogue
static void cmd_where_traverse(context *ctx, uint64_t rbp, uint64_t rip) {
  char buf[64];
  // reaches top (also can check if top main reached)
  if (rbp == 1) {
    return;
  }

  debug_sub *sub = context_dbg_sub_by_address(ctx, rip);
  if (!sub) {
    context_error(ctx, "no debug info about current subroutine");
    return;
  }

  snprintf(buf, STRMAXLEN(buf), "0x%016lx: ", rip);
  context_out_append(ctx, buf);

  context_out_append(ctx, context_dbg_str_by_id(ctx, sub->strs_index));

  context_out_append(ctx, " (");
  for (uint64_t i = 0; i < sub->args_cnt; ++i) {
    debug_arg *arg = sub->args + i;

    // TODO: print value string
    UNUSED(arg);

    // cmd_value_str value = cmd_print_value_str(ctx, rbp + arg->rbp_offset);
    // context_out_append(ctx, context_dbg_str_by_id(ctx, arg->strs_index));
    // if (value.type) {
    //   context_out_append(ctx, ": ");
    //   context_out_append(ctx, value.type);
    //   free(value.type);
    // }
    // context_out_append(ctx, " = ");
    // if (value.value) {
    //   context_out_append(ctx, value.value);
    //   free(value.value);
    // }
  }
  context_out_appendln(ctx, ")");

  uint64_t p_rip;
  if (context_tg_peekdata(ctx, cmd_where_p_rip(rbp), &p_rip) == TARGET_ERROR) {
    context_error(ctx, "unable to peek data");
    return;
  }

  uint64_t p_rbp;
  if (context_tg_peekdata(ctx, cmd_where_p_rbp(rbp), &p_rbp) == TARGET_ERROR) {
    context_error(ctx, "unable to peek data");
    return;
  }

  cmd_where_traverse(ctx, p_rbp, p_rip);
}

static void cmd_where_fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(rest);
  struct user_regs_struct regs;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  if (context_tg_getregs(ctx, &regs) == TARGET_ERROR) {
    context_error(ctx, "failed to get regs");
    goto cleanup;
  }

  cmd_where_traverse(ctx, regs.rbp, regs.rip);

cleanup:
  return;
}

cmd *cmd_where(const char *cmd_ref) {
  return cmd_entry_new(cmd_where_fn, DESC, HELP, cmd_ref);
}
