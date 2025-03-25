#include "cmd.h"
#include "util/macro.h"

static const char *DESC = "Echo command";

static const char *HELP = "Usage: <command> <arg>...";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  context_out_append(ctx, rest);
}

cmd *cmd_echo(const char *cmd_ref) {
  return cmd_entry_new(fn, DESC, HELP, cmd_ref);
}
