#include "debugger/cmd/cmd.h"

#include "debugger/context/context.h"
#include "util/macro.h"
#include "util/string.h"

#include <ctype.h>
#include <dis-asm.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/user.h>
#include <sys/wait.h>

static const char *desc =
    "Step one instruction, but proceed through subroutine calls";

static const char *help = "Usage: <command> [<n>]\n"
                          "\t<n>: number of times";

static int fn_fprintf(void *stream, const char *fmt, ...) {
  UNUSED(stream);
  UNUSED(fmt);
  return 0;
}

static int fn_fprintf_styled_ftype(void *stream, enum disassembler_style style,
                                   const char *fmt, ...) {
  char    buf[64];
  va_list args;

  va_start(args, fmt);

  switch (style) {
  case dis_style_mnemonic:
  case dis_style_sub_mnemonic:
    vsnprintf(buf, STRMAXLEN(buf), fmt, args);
    if (!strncmp(buf, "call", 4) && (isspace(buf[4]) || buf[4] == '\0')) {
      *((int *)stream) = 1;
    }
    break;
  default:
    break;
  }

  va_end(args);

  return 0;
}

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char                   *args = NULL;
  size_t                  n = 1;
  struct user_regs_struct regs;
  struct disassemble_info info;
  const uint64_t          int3 = 0xCC;
  int                     is_call;

  args = trim(strdup(rest));

  if (args && *args) {
    sscanf(args, "%lu", &n);
  }

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    goto cleanup;
  }

  init_disassemble_info(&info, &is_call, fn_fprintf, fn_fprintf_styled_ftype);

  info.arch = bfd_arch_i386;
  info.mach = bfd_mach_x86_64;
  info.application_data = ctx;
  info.read_memory_func = cmd_read_memory;
  info.memory_error_func = cmd_memory_error;

  disassembler_ftype disasm =
      disassembler(bfd_arch_i386, false, bfd_mach_x86_64, NULL);

  if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs) == -1) {
    cmd_error(ctx, "failed to get regs");
    goto cleanup;
  }

  for (size_t i = 0; i < n; ++i) {
    is_call = 0;
    uint64_t data = 0;

    bfd_vma pc = regs.rip;
    size_t  insn_size = disasm(pc, &info);
    pc += insn_size;

    if (is_call) {
      errno = 0;
      data = ptrace(PTRACE_PEEKDATA, ctx->pid, pc, NULL);
      if (errno) {
        cmd_error(ctx, "ptrace peek error");
        goto cleanup;
      }

      uint64_t data_int3 = ((data & ~0xFF) | int3);
      if (ptrace(PTRACE_POKEDATA, ctx->pid, pc, data_int3) == -1) {
        cmd_error(ctx, "ptrace set breakpoint error");
        goto cleanup;
      }

      if (ptrace(PTRACE_CONT, ctx->pid, NULL, NULL) == -1) {
        cmd_error(ctx, "ptrace cont error");
        goto cleanup;
      }
    } else {
      if (ptrace(PTRACE_SINGLESTEP, ctx->pid, NULL, NULL) == -1) {
        cmd_error(ctx, "ptrace singlestep error");
        goto cleanup;
      }
    }

    int status;
    int wres = waitpid(ctx->pid, &status, 0);
    if (wres == -1) {
      cmd_error(ctx, "waitpid error");
      goto cleanup;
    } else if (WIFEXITED(status) || WIFSIGNALED(status)) {
      ctx->pid = 0;
    }

    if (context_get_target_state(ctx) != TARGET_RUNNING) {
      break;
    }

    if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs)) {
      cmd_error(ctx, "ptrace getregs after break error");
      goto cleanup;
    }

    if (is_call) {
      regs.rip -= 1;

      if (ptrace(PTRACE_SETREGS, ctx->pid, NULL, &regs)) {
        cmd_error(ctx, "ptrace setregs after break error");
        goto cleanup;
      }

      if (ptrace(PTRACE_POKEDATA, ctx->pid, pc, data) == -1) {
        cmd_error(ctx, "ptrace remove breakpoint error");
        goto cleanup;
      }
    }
  }

  if (context_get_target_state(ctx) == TARGET_RUNNING) {
    cmd_disassemble_at_address(ctx, regs.rip, 1);
  }

cleanup:
  if (args) {
    free(args);
  }
}

cmd *cmd_nexti(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
