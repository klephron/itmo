#include "debugger/cmd/cmd.h"

#include "debugger/context/context.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"

#include <errno.h>
#include <stdlib.h>
#include <sys/ptrace.h>

static const char *desc = "Set breakpoint at specific location";

static const char *help = "Usage: <command> <address>\n"
                          "\t<address>: address in hex";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char          *args = NULL;
  char          *arg_address = NULL;
  const uint64_t int3 = 0xCC;

  args = trim(strdup(rest));

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  {
    char *token_args = strdup(args);
    char *token = strtok(token_args, " ");
    if (token) {
      arg_address = strdup(token);
      debug(arg_address);
      token = strtok(NULL, " ");
    }
  }

  if (!arg_address) {
    goto error_format;
  }

  uint64_t address;
  sscanf(arg_address, "%lx", &address);

  uint64_t data = ptrace(PTRACE_PEEKDATA, ctx->pid, address, NULL);
  if (errno) {
    cmd_error(ctx, "ptrace peek error");
    goto cleanup;
  }

  uint64_t data_int3 = ((data & ~0xFF) | int3);
  if (ptrace(PTRACE_POKEDATA, ctx->pid, address, data_int3) == -1) {
    cmd_error(ctx, "ptrace set breakpoint error");
  }
  goto cleanup;

error_format:
  cmd_error_format(ctx, args, help);

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_break(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
