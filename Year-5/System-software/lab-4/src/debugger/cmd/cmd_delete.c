#include "debugger/cmd/cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"
#include "util/string.h"

static const char *desc = "Delete breakpoint by id or all";

static const char *help = "Usage: <command> [<id>]\n"
                          "\t<id>: id of breakpoint";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *args = NULL;
  char *arg_id = NULL;

  args = trim(strdup(rest));

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  {
    char *token_args = strdup(args);
    char *token = strtok(token_args, " ");
    if (token) {
      arg_id = strdup(token);
      token = strtok(NULL, " ");
    }
    if (token_args) {
      free(token_args);
    }
  }

  uint64_t id;
  if (!arg_id || !*arg_id) {
    id = 0;
  } else {
    sscanf(arg_id, "%lu", &id);
  }

  size_t deleted = 0;

  for (hashset_breakpoint_it it = hashset_breakpoint_begin(ctx->breaks);
       !it.end(&it); it.next(&it)) {

    breakpoint *b = it.get(&it);

    if (!id || b->id == id) {
      ++deleted;
      if (context_tg_is_running(ctx)) {
        context_bp_remove(ctx, b);
      }
      if (id) {
        hashset_breakpoint_erase(ctx->breaks, it);
        break;
      }
    }
  }

  if (!id) {
    hashset_breakpoint_free(ctx->breaks);
    ctx->breaks = hashset_breakpoint_new();
  }

  if (!deleted) {
    context_error(ctx, "no breakpoints match");
    goto cleanup;
  }

cleanup:
  if (arg_id) {
    free(arg_id);
  }
  if (args) {
    free(args);
  }
}

cmd *cmd_delete(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
