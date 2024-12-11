#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "debugger/cmd/cmd.h"
#include "debugger/context/context.h"

static char *read_string() {
  char  *line = NULL;
  size_t len = 0;
  getline(&line, &len, stdin);
  if (line == NULL) {
    return NULL;
  }
  return line;
}

static void add_cmds(cmd_handler *handler) {
  cmd_handler_cmd_add(handler, cmd_entry_new("exit", cmd_exit));
  cmd_handler_cmd_add(handler, cmd_entry_new("echo", cmd_echo));
  cmd_handler_cmd_add(handler, cmd_entry_new("file", cmd_file));
  cmd_handler_cmd_add(handler, cmd_entry_new("run", cmd_run));
  cmd_handler_cmd_add(handler, cmd_entry_new("info", cmd_info));
  cmd_handler_cmd_add(handler, cmd_entry_new("kill", cmd_kill));
}

int main() {
  context *ctx = context_new();

  cmd_handler *handler = cmd_handler_new();
  add_cmds(handler);

  while (ctx->s_debugger != STATE_DEBUGGER_EXITING) {
    fprintf(stdout, "(dbg) ");

    char *cmd = read_string();
    cmd_handler_handle(handler, ctx, cmd);
    free(cmd);

    if (string_buffer_size(ctx->buf_out)) {
      fprintf(stdout, "%s", string_buffer_data(ctx->buf_out));
      string_buffer_reset(ctx->buf_out);
    }

    if (ctx->s_debugger == STATE_DEBUGGER_ERROR) {
      fprintf(stderr, "%s", string_buffer_data(ctx->buf_err));
      string_buffer_reset(ctx->buf_err);
      ctx->s_debugger = STATE_DEBUGGER_IDLE;
    }
  }

  cmd_handler_free(handler);
  context_free(ctx);
}
