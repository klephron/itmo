#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>

static const char *desc = "Kill running target";

static const char *help = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *args = NULL;
  int   sig  = SIGKILL;
  char  buf[64];

  args = trim(strdup(rest));
  if (args && *args) {
    sig = atoi(args);
  }

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  snprintf(buf, STRMAXLEN(buf), "%d", ctx->pid);

  if (context_tg_kill(ctx, sig) != TARGET_ERROR) {
    context_out_append(ctx, "killed child with pid ");
    context_out_appendln(ctx, buf);
  }

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_kill(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
