#include "cmd.h"

#include "debugger/context/context.h"
#include "debugger/context/str.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"
#include <bfd.h>
#include <dis-asm.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/user.h>
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

void cmd_exit(context *ctx, const char *rest) {
  UNUSED(rest);

  ctx->s_debugger = STATE_DEBUGGER_EXITING;
}

void cmd_echo(context *ctx, const char *rest) { context_out_append(ctx, rest); }

void cmd_file(context *ctx, const char *rest) {
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

static void cmd_info_register_g(context *ctx, struct user_regs_struct *regs,
                                const char *reg, size_t offset) {
  char                buf[64];
  unsigned long long *addr = (void *)regs + offset;

  context_out_append(ctx, reg);

  context_out_append(ctx, "\t");
  snprintf(buf, STRMAXLEN(buf), "0x%-16llx", *addr);
  context_out_append(ctx, buf);

  context_out_append(ctx, "\t");
  snprintf(buf, STRMAXLEN(buf), "%-16lld", *addr);
  context_out_append(ctx, buf);

  context_out_appendln(ctx, "");
}

void cmd_info(context *ctx, const char *rest) {
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
  } else if (!strcmp(arg, "registers")) {
    if (do_error_no_target(ctx)) {
      goto cleanup;
    }
    if (ctx->s_target == STATE_TARGET_NOT_RUNNING) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_appendln(ctx, "target is not running");
      goto cleanup;
    }

    struct user_regs_struct regs;

    if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs) == -1) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_appendln(ctx, "failed to get regs");
      goto cleanup;
    }

    cmd_info_register_g(ctx, &regs, "rax",
                        offsetof(struct user_regs_struct, rax));
    cmd_info_register_g(ctx, &regs, "rbx",
                        offsetof(struct user_regs_struct, rbx));
    cmd_info_register_g(ctx, &regs, "rcx",
                        offsetof(struct user_regs_struct, rcx));
    cmd_info_register_g(ctx, &regs, "rdx",
                        offsetof(struct user_regs_struct, rdx));
    cmd_info_register_g(ctx, &regs, "rsi",
                        offsetof(struct user_regs_struct, rsi));
    cmd_info_register_g(ctx, &regs, "rdi",
                        offsetof(struct user_regs_struct, rdi));
    cmd_info_register_g(ctx, &regs, "rbp",
                        offsetof(struct user_regs_struct, rbp));
    cmd_info_register_g(ctx, &regs, "rsp",
                        offsetof(struct user_regs_struct, rsp));
    cmd_info_register_g(ctx, &regs, "r8",
                        offsetof(struct user_regs_struct, r8));
    cmd_info_register_g(ctx, &regs, "r9",
                        offsetof(struct user_regs_struct, r9));
    cmd_info_register_g(ctx, &regs, "r10",
                        offsetof(struct user_regs_struct, r10));
    cmd_info_register_g(ctx, &regs, "r11",
                        offsetof(struct user_regs_struct, r11));
    cmd_info_register_g(ctx, &regs, "r12",
                        offsetof(struct user_regs_struct, r12));
    cmd_info_register_g(ctx, &regs, "r13",
                        offsetof(struct user_regs_struct, r13));
    cmd_info_register_g(ctx, &regs, "r14",
                        offsetof(struct user_regs_struct, r14));
    cmd_info_register_g(ctx, &regs, "r15",
                        offsetof(struct user_regs_struct, r15));
    cmd_info_register_g(ctx, &regs, "rip",
                        offsetof(struct user_regs_struct, rip));
    cmd_info_register_g(ctx, &regs, "eflags",
                        offsetof(struct user_regs_struct, eflags));
    cmd_info_register_g(ctx, &regs, "cs",
                        offsetof(struct user_regs_struct, cs));
    cmd_info_register_g(ctx, &regs, "ss",
                        offsetof(struct user_regs_struct, ss));
    cmd_info_register_g(ctx, &regs, "ds",
                        offsetof(struct user_regs_struct, ds));
    cmd_info_register_g(ctx, &regs, "es",
                        offsetof(struct user_regs_struct, es));
    cmd_info_register_g(ctx, &regs, "fs",
                        offsetof(struct user_regs_struct, fs));
    cmd_info_register_g(ctx, &regs, "gs",
                        offsetof(struct user_regs_struct, gs));

  } else {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "unknown args ");
    context_err_appendln(ctx, arg);
    goto cleanup;
  }

cleanup:
  if (arg) {
    free(arg);
  }
}

void cmd_run(context *ctx, const char *rest) {
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

void cmd_kill(context *ctx, const char *rest) {
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

void cmd_print(context *ctx, const char *rest) {
  struct user_regs_struct regs;
  char                    buf[64];

  char *args = trim(strdup(rest));

  if (do_error_no_target(ctx)) {
    goto cleanup;
  }

  if (!args || !*args) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "no args specified");
    goto cleanup;
  }

  if (ctx->s_target == STATE_TARGET_NOT_RUNNING) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not running");
    goto cleanup;
  }

  if (args[0] == '$') {
    char *reg = args + 1;

    if (ptrace(PTRACE_GETREGS, ctx->pid, NULL, &regs) == -1) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_appendln(ctx, "failed to get regs");
      goto cleanup;
    }

    size_t offset = SIZE_MAX;

    if (!(strcmp(reg, "rax"))) {
      offset = offsetof(struct user_regs_struct, rax);
    } else if (!(strcmp(reg, "rbx"))) {
      offset = offsetof(struct user_regs_struct, rbx);
    } else if (!(strcmp(reg, "rcx"))) {
      offset = offsetof(struct user_regs_struct, rcx);
    } else if (!(strcmp(reg, "rdx"))) {
      offset = offsetof(struct user_regs_struct, rdx);
    } else if (!(strcmp(reg, "rsi"))) {
      offset = offsetof(struct user_regs_struct, rsi);
    } else if (!(strcmp(reg, "rdi"))) {
      offset = offsetof(struct user_regs_struct, rdi);
    } else if (!(strcmp(reg, "rbp"))) {
      offset = offsetof(struct user_regs_struct, rbp);
    } else if (!(strcmp(reg, "rsp"))) {
      offset = offsetof(struct user_regs_struct, rsp);
    } else if (!(strcmp(reg, "r8"))) {
      offset = offsetof(struct user_regs_struct, r8);
    } else if (!(strcmp(reg, "r9"))) {
      offset = offsetof(struct user_regs_struct, r9);
    } else if (!(strcmp(reg, "r10"))) {
      offset = offsetof(struct user_regs_struct, r10);
    } else if (!(strcmp(reg, "r11"))) {
      offset = offsetof(struct user_regs_struct, r11);
    } else if (!(strcmp(reg, "r12"))) {
      offset = offsetof(struct user_regs_struct, r12);
    } else if (!(strcmp(reg, "r13"))) {
      offset = offsetof(struct user_regs_struct, r13);
    } else if (!(strcmp(reg, "r14"))) {
      offset = offsetof(struct user_regs_struct, r14);
    } else if (!(strcmp(reg, "r15"))) {
      offset = offsetof(struct user_regs_struct, r15);
    } else if (!(strcmp(reg, "rip"))) {
      offset = offsetof(struct user_regs_struct, rip);
    } else if (!(strcmp(reg, "eflags"))) {
      offset = offsetof(struct user_regs_struct, eflags);
    } else if (!(strcmp(reg, "cs"))) {
      offset = offsetof(struct user_regs_struct, cs);
    } else if (!(strcmp(reg, "ss"))) {
      offset = offsetof(struct user_regs_struct, ss);
    } else if (!(strcmp(reg, "ds"))) {
      offset = offsetof(struct user_regs_struct, ds);
    } else if (!(strcmp(reg, "es"))) {
      offset = offsetof(struct user_regs_struct, es);
    } else if (!(strcmp(reg, "fs"))) {
      offset = offsetof(struct user_regs_struct, fs);
    } else if (!(strcmp(reg, "gs"))) {
      offset = offsetof(struct user_regs_struct, gs);
    }

    if (offset == SIZE_MAX) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_append(ctx, "unknown register ");
      context_err_appendln(ctx, reg);
      goto cleanup;
    }

    snprintf(buf, STRMAXLEN(buf), "%#llx",
             *(unsigned long long *)(((void *)&regs) + offset));
    context_out_appendln(ctx, buf);
  } else {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_append(ctx, "unknown args ");
    context_err_appendln(ctx, args);
  }

cleanup:
  if (args) {
    free(args);
  }
}

static void cmd_x_print_le_8bytes(context *ctx, uint64_t data, char format,
                                  size_t size) {
  char   buf[64];
  size_t offset = 0;

  while (offset != 8) {
    switch (size) {
    case 8: {
      uint64_t value = data;
      switch (format) {
      case 'x':
        snprintf(buf, STRMAXLEN(buf), "0x%lx", value);
        break;
      case 'z':
        snprintf(buf, STRMAXLEN(buf), "0x%016lx", value);
        break;
      case 'd':
        snprintf(buf, STRMAXLEN(buf), "%ld", value);
        break;
      case 'u':
        snprintf(buf, STRMAXLEN(buf), "%lu", value);
        break;
      default:
        warn("unexpected format %c", format);
      }
      break;
    }
    case 4: {
      uint32_t value = data & 0xFFFFFFFF;
      data >>= 32;
      switch (format) {
      case 'x':
        snprintf(buf, STRMAXLEN(buf), "0x%x", value);
        break;
      case 'z':
        snprintf(buf, STRMAXLEN(buf), "0x%08x", value);
        break;
      case 'd':
        snprintf(buf, STRMAXLEN(buf), "%d", value);
        break;
      case 'u':
        snprintf(buf, STRMAXLEN(buf), "%u", value);
        break;
      default:
        warn("unexpected format %c", format);
      }
      break;
    }
    case 2: {
      uint16_t value = data & 0xFFFF;
      data >>= 16;
      switch (format) {
      case 'x':
        snprintf(buf, STRMAXLEN(buf), "0x%hx", value);
        break;
      case 'z':
        snprintf(buf, STRMAXLEN(buf), "0x%04hx", value);
        break;
      case 'd':
        snprintf(buf, STRMAXLEN(buf), "%hd", value);
        break;
      case 'u':
        snprintf(buf, STRMAXLEN(buf), "%hu", value);
        break;
      default:
        warn("unexpected format %c", format);
      }
      break;
    }
    case 1: {
      uint8_t value = data & 0xFF;
      data >>= 8;
      switch (format) {
      case 'x':
        snprintf(buf, STRMAXLEN(buf), "0x%hhx", value);
        break;
      case 'z':
        snprintf(buf, STRMAXLEN(buf), "0x%02hhx", value);
        break;
      case 'd':
        snprintf(buf, STRMAXLEN(buf), "%hhd", value);
        break;
      case 'u':
        snprintf(buf, STRMAXLEN(buf), "%hhu", value);
        break;
      default:
        warn("unexpected format %c", format);
      }
      break;
    }
    default:
      warn("unsupported size");
      return;
    }
    offset += size;
    context_out_append(ctx, buf);
    context_out_append(ctx, " ");
  }
}

void cmd_x(context *ctx, const char *rest) {
  char *args = trim(strdup(rest));
  char *arg_n = NULL;
  char *arg_format = NULL;
  char *arg_size = NULL;
  char *arg_address = NULL;

  if (do_error_no_target(ctx)) {
    goto cleanup;
  }

  if (ctx->s_target == STATE_TARGET_NOT_RUNNING) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not running");
    goto cleanup;
  }

  {
    char *token_args = strdup(args);
    char *token = strtok(token_args, " ");
    if (token) {
      arg_n = strdup(token);
      token = strtok(NULL, " ");
    }
    if (token) {
      arg_format = strdup(token);
      token = strtok(NULL, " ");
    }
    if (token) {
      arg_size = strdup(token);
      token = strtok(NULL, " ");
    }
    if (token) {
      arg_address = strdup(token);
      token = strtok(NULL, " ");
    }
  }

  if (!arg_n || !arg_format || !arg_size || !arg_address) {
    goto err_format;
  }

  size_t n;
  sscanf(arg_n, "%lu", &n);

  char format;
  if (strlen(arg_format) != 1) {
    goto err_format;
  } else {
    format = arg_format[0];
    switch (format) {
    case 'x':
    case 'z':
    case 'd':
    case 'u':
      break;
    default:
      goto err_format;
    }
  }

  size_t size;
  if (strlen(arg_size) != 1) {
    goto err_format;
  } else {
    switch (arg_size[0]) {
    case 'b':
      size = 1;
      break;
    case 'h':
      size = 2;
      break;
    case 'w':
      size = 4;
      break;
    case 'g':
      size = 8;
      break;
    default:
      goto err_format;
    }
  }

  uint64_t address;
  sscanf(arg_address, "%lx", &address);
  char buf[64];

  for (uint64_t addr_cur = address, addr_end = address + (n * size);
       addr_cur <= addr_end; addr_cur += sizeof(uint64_t)) {
    uint64_t data = ptrace(PTRACE_PEEKDATA, ctx->pid, addr_cur, NULL);
    snprintf(buf, STRMAXLEN(buf), "0x%lx: ", addr_cur);
    context_out_append(ctx, buf);
    cmd_x_print_le_8bytes(ctx, data, format, size);
    context_out_appendln(ctx, "");
  }

  return;

err_format:
  ctx->s_debugger = STATE_DEBUGGER_ERROR;
  context_err_append(ctx, "unexpected args ");
  context_err_appendln(ctx, args);
  context_err_appendln(ctx, "Arguments: <n> <format> <size> <start>");
  context_err_appendln(ctx, "\t<n>: elements count");
  context_err_appendln(ctx, "\t<format>: x(hex)|z(hex-left)|d(decimal)");
  context_err_appendln(ctx, "\t<size>: b(byte)|h(halfword)|w(word)|g(giant)");
  context_err_appendln(ctx, "\t<start>: start address in hex");
  goto cleanup;

cleanup:
  if (args) {
    free(args);
  }
  if (arg_n) {
    free(arg_n);
  }
  if (arg_format) {
    free(arg_format);
  }
  if (arg_size) {
    free(arg_size);
  }
  if (arg_address) {
    free(arg_address);
  }
  return;
}

static int cmd_disassemble_fprintf(void *stream, const char *fmt, ...) {
  char buf[256];

  string_buffer *buffer = (string_buffer *)stream;

  va_list args;
  va_start(args, fmt);

  int len = vsnprintf(buf, STRMAXLEN(buf), fmt, args);
  va_end(args);

  string_buffer_append(buffer, buf);
  return len;
}

static int cmd_disassemble_fprintf_styled_ftype(void                   *stream,
                                                enum disassembler_style style,
                                                const char *fmt, ...) {
  UNUSED(style);
  char           buf[256];
  string_buffer *buffer = (string_buffer *)stream;

  va_list args;
  va_start(args, fmt);

  int len = vsnprintf(buf, STRMAXLEN(buf), fmt, args);
  va_end(args);

  string_buffer_append(buffer, buf);
  return len;
}

static int cmd_disassemble_read_memory(bfd_vma memaddr, bfd_byte *addr,
                                       unsigned int             length,
                                       struct disassemble_info *info) {
  context *ctx = (context *)info->application_data;

  uint64_t bytes = 0;
  for (bfd_vma addr_cur = memaddr; bytes < length; addr_cur += sizeof(long)) {
    long data = ptrace(PTRACE_PEEKDATA, ctx->pid, addr_cur, NULL);
    if (errno) {
      ctx->s_debugger = STATE_DEBUGGER_ERROR;
      context_err_appendln(ctx, "ptrace error while fetching data");
      return 1;
    }
    uint64_t bytes_to_copy = sizeof(long);
    if (bytes + bytes_to_copy > length) {
      bytes_to_copy = length - bytes;
    }

    memcpy(addr + bytes, &data, bytes_to_copy);
    bytes += bytes_to_copy;
  }
  return 0;
}

static void cmd_disassemble_memory_error(int status, bfd_vma memaddr,
                                         struct disassemble_info *info) {
  char buf[256];

  snprintf(buf, STRMAXLEN(buf), "memory error at address 0x%lx, status: %d\n",
           (long)memaddr, status);
  context_err_appendln((context *)info->application_data, buf);
}

void cmd_disassemble(context *ctx, const char *rest) {
  char *args = trim(strdup(rest));
  char *arg_address = NULL;
  char *arg_size = NULL;

  if (do_error_no_target(ctx)) {
    goto cleanup;
  }

  if (ctx->s_target == STATE_TARGET_NOT_RUNNING) {
    ctx->s_debugger = STATE_DEBUGGER_ERROR;
    context_err_appendln(ctx, "target is not running");
    goto cleanup;
  }

  {
    char *token_args = strdup(args);
    char *token = strtok(token_args, " ");
    if (token) {
      arg_address = strdup(token);
      token = strtok(NULL, " ");
    }
    if (token) {
      arg_size = strdup(token);
      token = strtok(NULL, " ");
    }
  }

  if (!arg_size || !arg_address) {
    goto err_format;
  }

  uint64_t address;
  sscanf(arg_address, "%lx", &address);

  uint64_t size;
  sscanf(arg_size, "%lu", &size);

  string_buffer *buffer = string_buffer_new(0, 0);

  struct disassemble_info info;
  init_disassemble_info(&info, buffer, cmd_disassemble_fprintf,
                        cmd_disassemble_fprintf_styled_ftype);

  info.arch = bfd_arch_i386;
  info.mach = bfd_mach_x86_64;

  info.read_memory_func = cmd_disassemble_read_memory;
  info.memory_error_func = cmd_disassemble_memory_error;

  info.application_data = ctx;
  disassembler_ftype disasm =
      disassembler(bfd_arch_i386, false, bfd_mach_x86_64, NULL);

  bfd_vma pc = address;
  while (pc < address + size) {
    size_t insn_size = disasm(pc, &info);
    pc += insn_size;
    if (pc < address + size) {
      string_buffer_append(buffer, "\n");
    }
  }

  char *data = string_buffer_detach(buffer);
  if (data) {
    context_out_appendln(ctx, data);
    free(data);
  }

  return;

err_format:
  ctx->s_debugger = STATE_DEBUGGER_ERROR;
  context_err_append(ctx, "unexpected args ");
  context_err_appendln(ctx, args);
  context_err_appendln(ctx, "Arguments: <start> <size>");
  context_err_appendln(ctx, "\t<start>: start address in hex");
  context_err_appendln(ctx, "\t<size>: b(byte)|h(halfword)|w(word)|g(giant)");
  goto cleanup;

cleanup:
  if (args) {
    free(args);
  }
  if (arg_size) {
    free(arg_size);
  }
  if (arg_address) {
    free(arg_address);
  }
  return;
}
