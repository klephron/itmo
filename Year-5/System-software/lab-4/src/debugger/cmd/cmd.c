#include "cmd.h"

#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"
#include <bfd.h>

void cmd_exit(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  UNUSED(rest);

  ctx->s_debugger = STATE_DEBUGGER_EXITING;
}

void cmd_echo(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);

  string_buffer_append(ctx->buf_out, rest);
  ctx->s_debugger = STATE_DEBUGGER_IDLE;
}

void cmd_file(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  bfd      *abfd = NULL;
  char     *name = NULL;
  asymbol **abfd_asymbols = NULL;

  name = trim(strdup(rest));
  if (!name || !*name) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    string_buffer_append(ctx->buf_err, "no file specified\n");
    goto cleanup;
  }

  bfd_init();

  abfd = bfd_openr(name, NULL);
  if (!abfd) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    string_buffer_append(ctx->buf_err, "failed to open file ");
    string_buffer_append(ctx->buf_err, name);
    string_buffer_append(ctx->buf_err, "\n");
    goto cleanup;
  }

  if (!bfd_check_format(abfd, bfd_object)) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    string_buffer_append(ctx->buf_err, "not an object file\n");
    goto cleanup;
  }

  int64_t abfd_asymbols_size = bfd_get_symtab_upper_bound(abfd);
  if (abfd_asymbols_size < 0) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    string_buffer_append(ctx->buf_err, "failed to get symbol table size\n");
    goto cleanup;
  }

  abfd_asymbols = malloc(abfd_asymbols_size);

  int64_t abfd_asymbols_count = bfd_canonicalize_symtab(abfd, abfd_asymbols);
  if (abfd_asymbols_count < 0) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    string_buffer_append(ctx->buf_err, "failed to canonicalize symbol table\n");
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
  if (ctx->name) {
    free(ctx->name);
  }
  ctx->name = name;

  ctx->s_debugger = STATE_DEBUGGER_IDLE;
  ctx->s_target = STATE_TARGET_NOT_RUNNING;

  return;

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

void cmd_info(context *ctx, const cmd_entry *entry, const char *rest) {
  char *arg;

  arg = trim(strdup(rest));

  if (!strcmp(arg, "sections")) {
    if (ctx->s_target == STATE_TARGET_NO_TARGET) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      string_buffer_append(ctx->buf_err, "target is not loaded\n");
      goto cleanup;
    }

    asection *section = ctx->abfd->sections;
    while (section) {
      char          buf[64];
      const char   *name = bfd_section_name(section);
      bfd_vma       address = bfd_section_vma(section);
      bfd_size_type size = bfd_section_size(section);
      string_buffer_append(ctx->buf_out, "name: ");
      string_buffer_append(ctx->buf_out, name);
      string_buffer_append(ctx->buf_out, ",\t");
      string_buffer_append(ctx->buf_out, "address: ");
      snprintf(buf, STRMAXLEN(buf), "%#lx", address);
      string_buffer_append(ctx->buf_out, buf);
      string_buffer_append(ctx->buf_out, ",\t");
      string_buffer_append(ctx->buf_out, "size(bytes): ");
      snprintf(buf, STRMAXLEN(buf), "%#lx", size);
      string_buffer_append(ctx->buf_out, buf);
      string_buffer_append(ctx->buf_out, "\n");
      section = section->next;
    }

  } else if (!strcmp(arg, "symbols")) {
    if (ctx->s_target == STATE_TARGET_NO_TARGET) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      string_buffer_append(ctx->buf_err, "target is not loaded\n");
      goto cleanup;
    }

    for (int64_t i = 0; i < ctx->abfd_asymbols_count; ++i) {
      char        buf[64];
      const char *name = bfd_asymbol_name(ctx->abfd_asymbols[i]);
      asection   *section = bfd_asymbol_section(ctx->abfd_asymbols[i]);
      const char *section_name = bfd_section_name(section);
      bfd_vma     address = bfd_asymbol_value(ctx->abfd_asymbols[i]);
      string_buffer_append(ctx->buf_out, "section: ");
      string_buffer_append(ctx->buf_out, section_name);
      string_buffer_append(ctx->buf_out, ",\t");
      string_buffer_append(ctx->buf_out, "name: ");
      string_buffer_append(ctx->buf_out, name);
      string_buffer_append(ctx->buf_out, ",\t");
      string_buffer_append(ctx->buf_out, "address: ");
      snprintf(buf, STRMAXLEN(buf), "%#lx", address);
      string_buffer_append(ctx->buf_out, buf);
      string_buffer_append(ctx->buf_out, "\n");
    }
  } else {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    string_buffer_append(ctx->buf_err, "unknown ");
    string_buffer_append(ctx->buf_err, entry->cmd_ref);
    string_buffer_append(ctx->buf_err, " argument: ");
    string_buffer_append(ctx->buf_err, arg);
    string_buffer_append(ctx->buf_err, "\n");
    goto cleanup;
  }

  ctx->s_debugger = STATE_DEBUGGER_IDLE;

cleanup:
  if (arg) {
    free(arg);
  }
}

void cmd_run(context *ctx, const cmd_entry *entry, const char *rest) {}
