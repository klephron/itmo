#include "cmd.h"
#include "util/macro.h"

static const char *desc = "Print stacktrace";

static const char *help = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(ctx);
  UNUSED(rest);
}

cmd *cmd_where(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
