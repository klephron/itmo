#pragma once

#include "debugger/context/context.h"
#include "util/container_util.h"
#include "util/hashset.h"

#include <stdint.h>
#include <stdlib.h>

typedef struct cmd_entry_struct cmd_entry;
// updates context state inside of cmd
typedef void (*cmd)(context *ctx, const char *rest);

typedef struct cmd_entry_struct {
  char *cmd_ref;
  cmd   handle_ref;
} cmd_entry;

cmd_entry *cmd_entry_new(char *cmd_ref, cmd handle_ref);
void       cmd_entry_free(cmd_entry *self);

static inline int container_cmp_cmd(const void *_lsv, const void *_rsv) {
  const cmd_entry *lsv = _lsv;
  const cmd_entry *rsv = _rsv;
  return container_cmp_chars(lsv->cmd_ref, rsv->cmd_ref);
}
static inline void container_delete_cmd(void *lsv) {
  (void)free;
  cmd_entry_free(lsv);
}
static inline uint64_t container_hash_cmd(const void *_lsv) {
  const cmd_entry *lsv = _lsv;
  uint64_t         hash = container_hash_chars(lsv->cmd_ref);
  return hash;
}
HASHSET_DECLARE_STATIC_INLINE(hashset_cmd, cmd_entry, container_cmp_cmd,
                              container_new_move, container_delete_cmd,
                              container_hash_cmd);

typedef struct cmd_handler_struct {
  hashset_cmd *cmds;
  cmd_entry   *cmd_prev;
  char        *cmd_prev_rest;
} cmd_handler;

cmd_handler *cmd_handler_new();
void         cmd_handler_free(cmd_handler *self);

void cmd_handler_register(cmd_handler *self, cmd_entry *entry);
void cmd_handler_handle(cmd_handler *self, context *ctx, const char *cmd);
