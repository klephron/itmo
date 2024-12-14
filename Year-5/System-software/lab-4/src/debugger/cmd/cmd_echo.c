#include "cmd.h"
#include "util/macro.h"

void cmd_echo(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  context_out_append(ctx, rest);
}

const char *cmd_echo_desc = "Echo command";
