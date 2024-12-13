#include "handler.h"

#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"
#include <ctype.h>
#include <stdlib.h>
#include <string.h>

cmd_entry *cmd_entry_new(char *cmd_ref, cmd handle_ref) {
  cmd_entry *self = MALLOC(cmd_entry);
  self->cmd_ref = cmd_ref;
  self->handle_ref = handle_ref;
  return self;
}

void cmd_entry_free(cmd_entry *self) {
  if (self) {
    free(self);
  }
}

cmd_handler *cmd_handler_new() {
  cmd_handler *self = MALLOC(cmd_handler);
  self->cmds = hashset_cmd_new();
  self->cmd_prev = NULL;
  self->cmd_prev_rest = NULL;
  return self;
}

void cmd_handler_free(cmd_handler *self) {
  if (self) {
    hashset_cmd_free(self->cmds);
    self->cmd_prev = NULL;
    if (self->cmd_prev_rest) {
      free(self->cmd_prev_rest);
    }
    free(self);
  }
}

void cmd_handler_register(cmd_handler *self, cmd_entry *entry) {
  hashset_cmd_insert(self->cmds, entry);
  debug("handler %p added cmd %s", self, entry->cmd_ref);
}

void cmd_handler_handle(cmd_handler *self, context *ctx, const char *cmd_ref) {
  char  c_tmp;
  char *cmd = strdup(cmd_ref);
  char *cmd_start = trim_start(cmd);
  char *cmd_rest = cmd_start;

  while (*cmd_rest && !isspace((unsigned char)*cmd_rest)) {
    ++cmd_rest;
  }
  // break string to hash it
  c_tmp = *cmd_rest;
  *cmd_rest = '\0';

  // repeat command if exists
  if (!*cmd_start) {
    if (self->cmd_prev) {
      self->cmd_prev->handle_ref(ctx, self->cmd_prev_rest);
    }
    goto cleanup;
  }

  hashset_cmd_it it =
      hashset_cmd_find(self->cmds, &(cmd_entry){.cmd_ref = (char *)cmd_start});

  if (it.end(&it)) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    string_buffer_append(ctx->buf_err, "unknown command: ");
    string_buffer_append(ctx->buf_err, cmd_start);
    string_buffer_append(ctx->buf_err, "\n");
    goto cleanup;
  }

  cmd_entry *entry = it.get(&it);

  *cmd_rest = c_tmp;
  while (isspace((unsigned char)*cmd_rest)) {
    ++cmd_rest;
  }

  ctx->s_debugger = STATE_DEBUGGER_PROCESSING;
  entry->handle_ref(ctx, cmd_rest);

  self->cmd_prev = entry;
  if (self->cmd_prev_rest) {
    free(self->cmd_prev_rest);
  }
  self->cmd_prev_rest = strdup(cmd_rest);

  if (ctx->s_debugger == STATE_DEBUGGER_PROCESSING) {
    ctx->s_debugger = STATE_DEBUGGER_IDLE;
  }

cleanup:
  if (cmd) {
    free(cmd);
  }
}
