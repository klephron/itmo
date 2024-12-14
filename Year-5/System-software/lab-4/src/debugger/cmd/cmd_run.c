#include "cmd.h"

#include "util/macro.h"
#include "util/string.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>

void cmd_run(context *ctx, const char *rest) {
  char  *args_str = NULL;
  char **argv = NULL;
  int    do_exit = 0;

  args_str = trim(strdup(rest));

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  if (context_get_target_state(ctx) == STATE_TARGET_RUNNING) {
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
