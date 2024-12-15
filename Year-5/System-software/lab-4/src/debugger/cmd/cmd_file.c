#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <stdlib.h>

static const char *desc = "Load target";

static const char *help = "Usage: <command> <file>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  bfd      *abfd = NULL;
  char     *args = NULL;
  asymbol **abfd_asymbols = NULL;

  args = trim(strdup(rest));
  if (!args || !*args) {
    cmd_error(ctx, "no file specified");
    goto help;
  }

  bfd_init();

  abfd = bfd_openr(args, NULL);
  if (!abfd) {
    cmd_error(ctx, "failed to open file");
    goto cleanup;
  }

  if (!bfd_check_format(abfd, bfd_object)) {
    cmd_error(ctx, "not an object file");
    goto cleanup;
  }

  int64_t abfd_asymbols_size = bfd_get_symtab_upper_bound(abfd);
  if (abfd_asymbols_size < 0) {
    cmd_error(ctx, "failed to get symbol table size");
    goto cleanup;
  }

  abfd_asymbols = malloc(abfd_asymbols_size);

  int64_t abfd_asymbols_count = bfd_canonicalize_symtab(abfd, abfd_asymbols);
  if (abfd_asymbols_count < 0) {
    cmd_error(ctx, "failed to canonicalize symbol table");
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
  ctx->fname = args;

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
  if (args) {
    free(args);
  }
}

cmd *cmd_file(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
