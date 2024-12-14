#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>

void cmd_kill(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *args_str = NULL;
  int   sig = SIGKILL;
  char  buf[64];

  args_str = trim(strdup(rest));
  if (args_str && *args_str) {
    sig = atoi(args_str);
  }

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  if (context_get_target_state(ctx) == STATE_TARGET_NOT_RUNNING) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "target is not running\n");
    goto cleanup;
  }

  snprintf(buf, STRMAXLEN(buf), "%d", ctx->pid);

  if (kill(ctx->pid, sig) == -1) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "failed to kill process with pid ");
    context_err_appendln(ctx, buf);
    goto cleanup;
  }

  int status;
  waitpid(ctx->pid, &status, 0);

  context_out_append(ctx, "killed ");
  context_out_append(ctx, buf);

  if (WIFSIGNALED(status)) {
    context_out_append(ctx, " by signal ");
    snprintf(buf, STRMAXLEN(buf), "%d", sig);
    context_out_append(ctx, buf);
    context_out_append(ctx, " with status ");
    snprintf(buf, STRMAXLEN(buf), "%d", WEXITSTATUS(status));
    context_out_appendln(ctx, buf);
  } else {
    context_out_appendln(ctx, " not by signal");
  }

cleanup:
  if (args_str) {
    free(args_str);
  }
}

const char *cmd_kill_desc = "Kill running target";
