#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <stdlib.h>

static const char *desc = "Load target";

static const char *help = "Usage: <command> <file>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  bfd      *abfd = NULL;
  char     *name = NULL;
  asymbol **abfd_asymbols = NULL;

  name = trim(strdup(rest));
  if (!name || !*name) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "no file specified");
    goto help;
  }

  bfd_init();

  abfd = bfd_openr(name, NULL);
  if (!abfd) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "failed to open file ");
    context_err_appendln(ctx, name);
    goto cleanup;
  }

  if (!bfd_check_format(abfd, bfd_object)) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "not an object file");
    goto cleanup;
  }

  int64_t abfd_asymbols_size = bfd_get_symtab_upper_bound(abfd);
  if (abfd_asymbols_size < 0) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "failed to get symbol table size");
    goto cleanup;
  }

  abfd_asymbols = malloc(abfd_asymbols_size);

  int64_t abfd_asymbols_count = bfd_canonicalize_symtab(abfd, abfd_asymbols);
  if (abfd_asymbols_count < 0) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "failed to canonicalize symbol table");
    goto cleanup;
  }

  ctx->abfd_asymbols_count = abfd_asymbols_count;
  if (ctx->abfd_asymbols) {
    free(ctx->abfd_asymbols);
  }
  ctx->abfd_asymbols = abfd_asymbols;
  if (ctx->abfd) {
    bfd_close(ctx->abfd);
  }
  ctx->abfd = abfd;
  if (ctx->fname) {
    free(ctx->fname);
  }
  ctx->fname = name;

  return;

help:
  context_err_appendln(ctx, help);

cleanup:
  if (abfd_asymbols) {
    free(abfd_asymbols);
  }
  if (abfd) {
    bfd_close(abfd);
  }
  if (name) {
    free(name);
  }
}

cmd *cmd_file(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
