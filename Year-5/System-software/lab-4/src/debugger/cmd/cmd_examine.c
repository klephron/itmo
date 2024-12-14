#include "cmd.h"

#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>

static void cmd_examine_le_8bytes(context *ctx, uint64_t data, char format,
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

void cmd_examine(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);
  char *args = trim(strdup(rest));
  char *arg_n = NULL;
  char *arg_format = NULL;
  char *arg_size = NULL;
  char *arg_address = NULL;

  if (cmd_error_if_no_target(ctx)) {
    goto cleanup;
  }

  if (context_get_target_state(ctx) == STATE_TARGET_NOT_RUNNING) {
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
    cmd_examine_le_8bytes(ctx, data, format, size);
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

const char *cmd_examine_desc = "Examine memory";
