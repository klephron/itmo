#include "debugger/cmd/cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"
#include "util/string.h"

static const char *desc = "Set breakpoint at specific location";

static const char *help = "Usage: <command> <address>\n"
                          "\t<address>: address in hex";

static void fn_out_breakpoint(context *ctx, breakpoint *breakpoint) {
  char buf[64];
  context_out_append(ctx, "breakpoint ");
  snprintf(buf, STRMAXLEN(buf), "%zu ", breakpoint->id);
  context_out_append(ctx, buf);
  snprintf(buf, STRMAXLEN(buf), "at 0x%016lx: ", breakpoint->address);
  context_out_append(ctx, buf);
  context_out_append(ctx, "file ");
  if (breakpoint->file) {
    context_out_append(ctx, breakpoint->file);
  } else {
    context_out_append(ctx, "-");
  }
  context_out_append(ctx, ", line: ");
  if (breakpoint->line != UINT64_MAX) {
    snprintf(buf, STRMAXLEN(buf), "%zu", breakpoint->line);
  } else {
    context_out_append(ctx, "-");
  }
  context_out_appendln(ctx, "");
}

static void fn(cmd_handler *handler, context *ctx, const char *rest) {

  UNUSED(handler);
  char *args = NULL;
  char *arg_address = NULL;

  args = trim(strdup(rest));

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  {
    char *token_args = strdup(args);
    char *token = strtok(token_args, " ");
    if (token) {
      arg_address = strdup(token);
      token = strtok(NULL, " ");
    }
    if (token_args) {
      free(token_args);
    }
  }

  if (!arg_address) {
    goto error_format;
  }

  uint64_t address;
  sscanf(arg_address, "%lx", &address);

  hashset_breakpoint_it it =
      hashset_breakpoint_find(ctx->breaks, &(breakpoint){.address = address});
  if (!it.end(&it)) {
    context_error(ctx, "breakpoint on this address already exists");
    goto cleanup;
  }

  breakpoint *breakpoint =
      breakpoint_new(++ctx->breaks_cnt, 0, address, NULL, UINT64_MAX);
  breakpoint->is_enabled = 1;

  hashset_breakpoint_insert(ctx->breaks, breakpoint);

  if (context_tg_is_running(ctx)) {
    context_bp_place(ctx, breakpoint);
  }

  fn_out_breakpoint(ctx, breakpoint);
  goto cleanup;

error_format:
  cmd_error_format(ctx, args, help);

cleanup:
  if (arg_address) {
    free(arg_address);
  }
  if (args) {
    free(args);
  }
}

cmd *cmd_break(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
