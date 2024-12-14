#include "cmd.h"

#include "debugger/cmd/handler.h"
#include "util/macro.h"
#include "util/math.h"

void cmd_help(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  UNUSED(ctx);
  UNUSED(rest);
  context_out_appendln(ctx, "Commands:");
  size_t first_w = 0;
  size_t entries_cnt = 0;
  for (hashset_cmd_it it = hashset_cmd_begin(handler->cmds); !it.end(&it);
       it.next(&it)) {
    cmd_entry *entry = it.get(&it);
    first_w = max_size_t(first_w, strlen(entry->cmd_ref));
    ++entries_cnt;
  }

  // sort elements
  cmd_entry **entries = malloc(sizeof(cmd_entry *) * entries_cnt);

  size_t i = 0;
  for (hashset_cmd_it it = hashset_cmd_begin(handler->cmds); !it.end(&it);
       it.next(&it)) {
    cmd_entry *entry = it.get(&it);
    entries[i++] = entry;
  }

  for (size_t i = 0; i < entries_cnt - 1; ++i) {
    for (size_t j = 0; j < entries_cnt - i - 1; ++j) {
      if (strcmp(entries[j]->cmd_ref, entries[j + 1]->cmd_ref) > 0) {
        cmd_entry *tmp = entries[j];
        entries[j] = entries[j + 1];
        entries[j + 1] = tmp;
      }
    }
  }

  for (size_t i = 0; i < entries_cnt; ++i) {
    cmd_entry *entry = entries[i];
    context_out_append(ctx, entry->cmd_ref);

    for (size_t i = 0; i < first_w - strlen(entry->cmd_ref); ++i) {
      context_out_append(ctx, " ");
    }
    context_out_append(ctx, " - ");
    context_out_appendln(ctx, entry->desc_ref);
  }
}

const char *cmd_help_desc = "Help";
