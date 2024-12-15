#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

static const char *desc = "Run target";

static const char *help = "Usage: <command>";

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char  *args = NULL;
  char **argv = NULL;

  args = trim(strdup(rest));

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  if (context_get_target_state(ctx) == TARGET_RUNNING) {
    cmd_error(ctx, "target is already running");
    goto cleanup;
  }

  // parse arguments to command
  size_t size = 4;
  size_t argc = 0;
  argv = malloc(size * sizeof(char *));
  char *token = strtok(args, " ");
  while (token) {
    if (argc >= size - 1) {
      size *= 2;
      argv = realloc(argv, size * sizeof(char *));
    }
    argv[argc++] = strdup(token);
    token = strtok(NULL, " ");
  }
  argv[argc] = NULL;

  pid_t pid = fork();
  if (pid == -1) {
    cmd_error(ctx, "error while forking");
    goto cleanup;
  }

  if (pid == 0) {
    if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
      perror("ptrace traceme");
      exit(1);
    }

    execvp(ctx->fname, argv);
    // on error
    perror("execvp");
    exit(1);
  } else {
    char buf[64];
    int  status;
    waitpid(pid, &status, 0);
    ctx->pid = pid;
    context_out_append(ctx, "started with pid ");
    snprintf(buf, STRMAXLEN(buf), "%d", pid);
    context_out_appendln(ctx, buf);
  }

cleanup:
  if (argv) {
    for (size_t i = 0; argv[i] != NULL; ++i) {
      free(argv[i]);
    }
    free(argv);
  }
  if (args) {
    free(args);
  }
}

cmd *cmd_run(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
