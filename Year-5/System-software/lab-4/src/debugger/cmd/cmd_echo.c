#include "cmd.h"
#include "util/macro.h"

static const char *desc = "Echo command";

static const char *help = "Usage: <command> <arg>...";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  context_out_append(ctx, rest);
}

cmd *cmd_echo(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
