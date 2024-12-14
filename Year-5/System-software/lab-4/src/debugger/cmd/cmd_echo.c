#include "cmd.h"

void cmd_echo(context *ctx, const char *rest) { context_out_append(ctx, rest); }
