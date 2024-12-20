#include "context.h"

#include <assert.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

int context_tg_is_running(context *self) {
  assert(self->s_target != TARGET_ERROR);
  switch (self->s_target) {
  case TARGET_BRKPT:
  case TARGET_TRAPPED:
    return 1;
  default:
    return 0;
  }
}

target_state_enum context_tg_get_state(context *self) {
  // debug("target state: %s", state_target_enum_str(self->s_target));
  assert(self->s_target != TARGET_ERROR);
  return self->s_target;
}

target_state_enum context_tg_set(context *self, uint64_t abfd_asymbols_count,
                                 asymbol **abfd_asymbols, bfd *abfd,
                                 char *fname) {
  if (context_tg_is_running(self)) {
    context_tg_kill(self, SIGKILL);
  }

  self->abfd_asymbols_count = abfd_asymbols_count;
  if (self->abfd_asymbols) {
    free(self->abfd_asymbols);
  }
  self->abfd_asymbols = abfd_asymbols;
  if (self->abfd) {
    bfd_close(self->abfd);
  }
  self->abfd = abfd;
  if (self->fname) {
    free(self->fname);
  }
  self->fname = fname;
  self->s_target = TARGET_NOT_RUNNING;

  context_dbg_deinit(self);
  context_dbg_init(self);

  return context_tg_get_state(self);
}

target_state_enum context_tg_wait(context *self) {
  int status;
  int wres = waitpid(self->pid, &status, 0);
  if (wres == -1) {
    return TARGET_ERROR;
  } else if (WIFEXITED(status) || WIFSIGNALED(status)) {
    self->pid = 0;
    self->s_target = TARGET_NOT_RUNNING;
  } else if (WIFSTOPPED(status) && WSTOPSIG(status) == SIGTRAP) {
    siginfo_t siginfo;
    if (ptrace(PTRACE_GETSIGINFO, self->pid, NULL, &siginfo) == 0) {
      if (siginfo.si_code == SI_KERNEL) {
        self->s_target = TARGET_BRKPT;
      } else {
        self->s_target = TARGET_TRAPPED;
      }
    }
  }
  return context_tg_get_state(self);
}

target_state_enum context_tg_kill(context *self, int signal) {
  if (kill(self->pid, signal) == -1) {
    return TARGET_ERROR;
  } else {
    return context_tg_wait(self);
  }
}

target_state_enum context_tg_continue(context *self) {
  // handle breakpoints
  context_tg_stepi(self);

  if (ptrace(PTRACE_CONT, self->pid, NULL, NULL) == -1) {
    return TARGET_ERROR;
  }
  return context_tg_wait(self);
}

target_state_enum context_tg_stepi(context *self) {
  struct user_regs_struct regs;
  target_state_enum       state_before;
  target_state_enum       state;
  breakpoint             *b = NULL;

  if ((state_before = context_tg_get_state(self)) == TARGET_BRKPT) {
    context_tg_brkpt_fix(self);
    self->s_target = TARGET_TRAPPED;
    context_tg_getregs(self, &regs);

    hashset_breakpoint_it it = hashset_breakpoint_find(
        self->breaks, &(breakpoint){.address = regs.rip});

    // in case of implicit breakpoints (handled not by ctx->breaks)
    if (!it.end(&it)) {
      b = it.get(&it);
      context_bp_remove(self, b);
    }
  }

  if (ptrace(PTRACE_SINGLESTEP, self->pid, NULL, NULL) == -1) {
    return TARGET_ERROR;
  }
  state = context_tg_wait(self);

  if (b) {
    context_bp_place(self, b);
  }

  return state;
}

// NOTE: assuming address is aligned to instructions (to check breakpoints)
target_state_enum context_tg_peekdata(context *self, uint64_t address,
                                      uint64_t *data_p) {
  uint64_t data;

  data = ptrace(PTRACE_PEEKDATA, self->pid, address, NULL);
  errno = 0;
  if (errno) {
    return TARGET_ERROR;
  }

  hashset_breakpoint_it it =
      hashset_breakpoint_find(self->breaks, &(breakpoint){.address = address});

  if (!it.end(&it)) {
    breakpoint *b = it.get(&it);
    if (b->is_placed) {
      data = peekdata_bpkpt_with_stored(data, b->data);
    }
  }

  *data_p = data;
  return context_tg_get_state(self);
}

// NOTE: assuming address is aligned to instructions (to check breakpoints)
target_state_enum context_tg_pokedata(context *self, uint64_t address,
                                      uint64_t data) {
  hashset_breakpoint_it b_it =
      hashset_breakpoint_find(self->breaks, &(breakpoint){.address = address});

  if (!b_it.end(&b_it)) {
    breakpoint *b = b_it.get(&b_it);
    if (b->is_placed) {
      b->data = peekdata_brkpt_to_store(data);
      data = peekdata_brkpt_with_int3(data);
    }
  }

  if (ptrace(PTRACE_POKEDATA, self->pid, address, data) == -1) {
    return TARGET_ERROR;
  }

  return context_tg_get_state(self);
}

target_state_enum context_tg_run(context *self, char **argv) {
  pid_t pid = fork();
  if (pid == -1) {
    return TARGET_ERROR;
  } else if (pid == 0) {
    if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
      perror("ptrace traceme");
      exit(1);
    }

    execvp(self->fname, argv);
    // on error
    perror("execvp");
    exit(1);
  } else {
    self->pid = pid;
    return context_tg_wait(self);
  }
  return context_tg_get_state(self);
}

target_state_enum context_tg_getregs(context                 *self,
                                     struct user_regs_struct *regs) {
  if (ptrace(PTRACE_GETREGS, self->pid, NULL, regs)) {
    return TARGET_ERROR;
  }
  return context_tg_get_state(self);
}

target_state_enum context_tg_setregs(context                 *self,
                                     struct user_regs_struct *regs) {
  if (ptrace(PTRACE_SETREGS, self->pid, NULL, regs)) {
    return TARGET_ERROR;
  }
  return context_tg_get_state(self);
}

// NOTE: can optimize place calls
target_state_enum context_tg_brkpt_fix(context *self) {
  struct user_regs_struct regs;
  context_tg_getregs(self, &regs);
  context_tg_brkpt_fix_regs(&regs);
  context_tg_setregs(self, &regs);
  return context_tg_get_state(self);
}

void context_tg_brkpt_fix_regs(struct user_regs_struct *regs_p) {
  regs_p->rip -= 1;
}

void context_tg_prev_line_reset(context *self) {
  self->prev_line.address = UINT64_MAX;
  self->prev_line.line = UINT64_MAX;
  self->prev_line.file_id = UINT64_MAX;
}

void context_tg_prev_line_set(context *self, debug_line line) {
  if (line.line != UINT64_MAX && line.file_id != UINT64_MAX) {
    self->prev_line = line;
  }
}

int context_tg_prev_line_changed(context *self, debug_line line) {
  if ((line.line != UINT64_MAX) && (line.file_id != UINT64_MAX)) {
    if ((line.file_id != self->prev_line.file_id) ||
        (line.line != self->prev_line.line)) {
      return 1;
    }
  }
  return 0;
}
