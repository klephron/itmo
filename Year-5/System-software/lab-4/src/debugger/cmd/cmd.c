#include "cmd.h"

#include "debugger/context/context.h"
#include "debugger/context/str.h"
#include "util/macro.h"
#include "util/string.h"
#include <bfd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

static int do_error_no_target(context *ctx) {
  if (ctx->s_target == STATE_TARGET_NO_TARGET) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not loaded");
    return 1;
  }
  return 0;
}

void cmd_exit(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  UNUSED(rest);

  ctx->s_debugger = STATE_DEBUGGER_EXITING;
}

void cmd_echo(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);

  context_out_append(ctx, rest);
}

void cmd_file(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  bfd      *abfd = NULL;
  char     *name = NULL;
  asymbol **abfd_asymbols = NULL;

  name = trim(strdup(rest));
  if (!name || !*name) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "no file specified");
    goto cleanup;
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
    if (do_error_no_target(ctx)) {
      goto cleanup;
    }

    asection *section = ctx->abfd->sections;
    while (section) {
      char          buf[64];
      const char   *name = bfd_section_name(section);
      bfd_vma       address = bfd_section_vma(section);
      bfd_size_type size = bfd_section_size(section);
      context_out_append(ctx, "name: ");
      context_out_append(ctx, name);
      context_out_append(ctx, ",\t");
      context_out_append(ctx, "address: ");
      snprintf(buf, STRMAXLEN(buf), "%#lx", address);
      context_out_append(ctx, buf);
      context_out_append(ctx, ",\t");
      context_out_append(ctx, "size: ");
      snprintf(buf, STRMAXLEN(buf), "%#lx", size);
      context_out_append(ctx, buf);
      context_out_appendln(ctx, " bytes");
      section = section->next;
    }

  } else if (!strcmp(arg, "symbols")) {
    if (do_error_no_target(ctx)) {
      goto cleanup;
    }

    for (int64_t i = 0; i < ctx->abfd_asymbols_count; ++i) {
      char        buf[64];
      const char *name = bfd_asymbol_name(ctx->abfd_asymbols[i]);
      asection   *section = bfd_asymbol_section(ctx->abfd_asymbols[i]);
      const char *section_name = bfd_section_name(section);
      bfd_vma     address = bfd_asymbol_value(ctx->abfd_asymbols[i]);
      context_out_append(ctx, "section: ");
      context_out_append(ctx, section_name);
      context_out_append(ctx, ",\t");
      context_out_append(ctx, "name: ");
      context_out_append(ctx, name);
      context_out_append(ctx, ",\t");
      context_out_append(ctx, "address: ");
      snprintf(buf, STRMAXLEN(buf), "%#lx", address);
      context_out_appendln(ctx, buf);
    }
  } else if (!strcmp(arg, "target")) {
    if (do_error_no_target(ctx)) {
      goto cleanup;
    }
    char buf[64];
    context_out_append(ctx, "file: ");
    context_out_append(ctx, ctx->fname);
    context_out_append(ctx, ", status: ");
    context_out_append(ctx, state_target_enum_str(ctx->s_target));
    if (ctx->s_target != STATE_TARGET_NOT_RUNNING) {
      context_out_append(ctx, ", pid: ");
      snprintf(buf, STRMAXLEN(buf), "%d", ctx->pid);
      context_out_appendln(ctx, buf);
    } else {
      context_out_appendln(ctx, "");
    }
  } else {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "unknown ");
    context_err_append(ctx, entry->cmd_ref);
    context_err_append(ctx, " argument: ");
    context_err_appendln(ctx, arg);
    goto cleanup;
  }

cleanup:
  if (arg) {
    free(arg);
  }
}

void cmd_run(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  char  *args_str = NULL;
  char **argv = NULL;
  int    do_exit = 0;

  args_str = trim(strdup(rest));

  if (do_error_no_target(ctx)) {
    goto cleanup;
  }

  if (ctx->s_target == STATE_TARGET_RUNNING) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is already running");
    goto cleanup;
  }

  pid_t pid = fork();
  if (pid == -1) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "error while forking");
    goto cleanup;
  }

  // child process
  if (pid == 0) {
    // parse arguments to command
    size_t size = 4;
    size_t argc = 0;
    argv = malloc(size * sizeof(char *));
    char *token = strtok(args_str, " ");
    while (token) {
      if (argc >= size - 1) {
        size *= 2;
        argv = realloc(argv, size * sizeof(char *));
      }
      argv[argc++] = strdup(token);
      token = strtok(NULL, " ");
    }
    argv[argc] = NULL;

    if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
      perror("ptrace traceme");
      do_exit = 1;
      goto cleanup;
    }

    execvp(ctx->fname, argv);
    // one error
    perror("execvp");
    do_exit = 1;
    goto cleanup;
  } else {
    char buf[64];
    int  status;
    waitpid(pid, &status, 0);
    ctx->s_target = STATE_TARGET_RUNNING;
    ctx->pid = pid;
    context_out_append(ctx, "started with pid ");
    snprintf(buf, STRMAXLEN(buf), "%d", pid);
    context_out_appendln(ctx, buf);
  }

cleanup:
  if (argv) {
    while (*argv) {
      free(*argv++);
    }
    free(argv);
  }
  if (args_str) {
    free(args_str);
  }
  if (do_exit) {
    exit(1);
  }
}

void cmd_kill(context *ctx, const cmd_entry *entry, const char *rest) {
  UNUSED(entry);
  char *args_str = NULL;
  int   sig = SIGKILL;
  char  buf[64];

  args_str = trim(strdup(rest));
  if (args_str && *args_str) {
    sig = atoi(args_str);
  }

  if (do_error_no_target(ctx)) {
    goto cleanup;
  }

  if (ctx->s_target == STATE_TARGET_NOT_RUNNING) {
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

  ctx->s_target = STATE_TARGET_NOT_RUNNING;

cleanup:
  if (args_str) {
    free(args_str);
  }
}
