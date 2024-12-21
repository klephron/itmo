#include "cmd.h"

#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"
#include "x86_64_core/value.h"

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ptrace.h>
#include <sys/user.h>

static const char *desc = "Print register, argument or variable";

static const char *help = "Usage: <command> (<register>|<arg>|<var>)\n"
                          "\t<register>: $<name>\n"
                          "\t<arg>: argument name\n"
                          "\t<var>: variable name";

static uint64_t cmd_align(uint64_t v) {
  return ((v - 1) / sizeof(v) + 1) * sizeof(v);
}

// NOTE: bytes should be % sizeof(uint64_t) == 0
static int cmd_peek_unaligned(context *ctx, uint64_t address, uint64_t bytes,
                              void *to) {
  uint64_t tmp;
  for (uint64_t i = 0; i < bytes; i += sizeof(uint64_t)) {
    if (context_tg_peekdata(ctx, address + i, &tmp) == TARGET_ERROR) {
      context_error(ctx, "unable to peek data");
      return -1;
    }
    memcpy((uint8_t *)to + i, &tmp, sizeof(tmp));
  }
  return 0;
}

static int cmd_peek_data_metadata(context *ctx, x86_64_type *type, uint64_t i) {
  x86_64_type_entry *entry = type->entries + i;
  switch (entry->kind) {
  case X86_64_TYPE_VOID:
    break;
  case X86_64_TYPE_BOOL:
    break;
  case X86_64_TYPE_BYTE:
    break;
  case X86_64_TYPE_INT:
    break;
  case X86_64_TYPE_UINT:
    break;
  case X86_64_TYPE_LONG:
    break;
  case X86_64_TYPE_ULONG:
    break;
  case X86_64_TYPE_CHAR:
    break;
  case X86_64_TYPE_STRING:
    break;
  case X86_64_TYPE_ARRAY: {
    x86_64_array_meta *meta = MALLOC(x86_64_array_meta);
    if (cmd_peek_unaligned(ctx, (uint64_t)entry->meta_array, sizeof(*meta),
                           meta) == -1) {
      warn("unable to peek array metadata");
      free(meta);
      meta = NULL;
    }
    entry->meta_array = meta;
    if (entry->i_child) {
      cmd_peek_data_metadata(ctx, type, entry->i_child);
    }
  } break;
  default:
    warn("unknown entry kind %d", entry->kind);
    break;
  }
  return 0;
}

static x86_64_data *cmd_peek_data(context *ctx, uint64_t address) {
  x86_64_data *data = NULL;
  x86_64_type *type = NULL;
  x86_64_data *result = NULL;
  x86_64_value value;
  assert(sizeof(value) % sizeof(uint64_t) == 0);

  // get value.data
  if (cmd_peek_unaligned(ctx, address, sizeof(value), &value.data) == -1) {
    goto cleanup;
  }

  if (!value.data) {
    goto cleanup;
  }

  // get x86_64_data header
  x86_64_data data_header;
  assert(sizeof(data_header) % sizeof(uint64_t) == 0);

  if (cmd_peek_unaligned(ctx, (uint64_t)value.data, sizeof(data_header),
                         &data_header) == -1) {
    warn("unable to copy data_header");
    goto cleanup;
  }

  // get x86_64_data
  uint64_t data_sz = cmd_align(sizeof(data_header) + data_header.raw_size);
  assert(data_sz % sizeof(uint64_t) == 0);

  data = malloc(data_sz);
  memcpy(data, &data_header, sizeof(data_header));
  if (cmd_peek_unaligned(ctx, (uint64_t)(value.data + 1),
                         data_sz - sizeof(data_header), data + 1) == -1) {
    warn("unable to copy data");
    goto cleanup;
  }

  // copy x86_64_type_header
  x86_64_type type_header;
  assert(sizeof(type_header) % sizeof(uint64_t) == 0);

  if (cmd_peek_unaligned(ctx, (uint64_t)data->type, sizeof(type_header),
                         &type_header) == -1) {
    warn("unable to copy type_header");
    goto cleanup;
  }

  // copy x86_64_type
  uint64_t type_sz = cmd_align(sizeof(type_header) +
                               type_header.count * sizeof(x86_64_type_entry));
  assert(data_sz % sizeof(uint64_t) == 0);

  type = malloc(type_sz);
  memcpy(type, &type_header, sizeof(type_header));
  if (cmd_peek_unaligned(ctx, (uint64_t)(data->type + 1),
                         type_sz - sizeof(type_header), type + 1) == -1) {
    warn("unable to copy type");
    goto cleanup;
  }
  cmd_peek_data_metadata(ctx, type, 0);

  data->type = type;
  data->refs = 1;
  result = data;

cleanup:
  if (!result && type) {
    free(type);
  }
  if (!result && data) {
    free(data);
  }
  return result;
}

static void cmd_data_str_type(string_buffer *buf, x86_64_type *type,
                              uint64_t ei) {
  x86_64_type_entry *entry = type->entries + ei;
  switch (entry->kind) {
  case X86_64_TYPE_VOID:
    string_buffer_append(buf, "void");
    break;
  case X86_64_TYPE_BOOL:
    string_buffer_append(buf, "bool");
    break;
  case X86_64_TYPE_BYTE:
    string_buffer_append(buf, "byte");
    break;
  case X86_64_TYPE_INT:
    string_buffer_append(buf, "int");
    break;
  case X86_64_TYPE_UINT:
    string_buffer_append(buf, "uint");
    break;
  case X86_64_TYPE_LONG:
    string_buffer_append(buf, "long");
    break;
  case X86_64_TYPE_ULONG:
    string_buffer_append(buf, "ulong");
    break;
  case X86_64_TYPE_CHAR:
    string_buffer_append(buf, "char");
    break;
  case X86_64_TYPE_STRING:
    string_buffer_append(buf, "string");
    break;
  case X86_64_TYPE_ARRAY:
    string_buffer_append(buf, "array[");
    cmd_data_str_type(buf, type, entry->i_child);
    string_buffer_append(buf, "]");
    break;
  case X86_64_TYPE_OBJECT:
    string_buffer_append(buf, "<object>");
    break;
  default:
    string_buffer_append(buf, "<unknown>");
    break;
  }
}

static void cmd_data_str_raw(string_buffer *buf, x86_64_type *type, uint64_t ei,
                             uint8_t *raw) {
  x86_64_type_entry *entry = type->entries + ei;
  char               tmp[64];
  switch (entry->kind) {
  case X86_64_TYPE_VOID:
    string_buffer_append(buf, "()");
    break;
  case X86_64_TYPE_BOOL: {
    uint8_t res = *raw;
    if (res) {
      snprintf(tmp, STRMAXLEN(tmp), "true");
    } else {
      snprintf(tmp, STRMAXLEN(tmp), "false");
    }
    string_buffer_append(buf, tmp);
  } break;
  case X86_64_TYPE_BYTE: {
    uint8_t res = *raw;
    snprintf(tmp, STRMAXLEN(tmp), "%d", res);
    string_buffer_append(buf, tmp);
  } break;
  case X86_64_TYPE_INT: {
    int32_t res = *(int32_t *)raw;
    snprintf(tmp, STRMAXLEN(tmp), "%d", res);
    string_buffer_append(buf, tmp);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t res = *(uint32_t *)raw;
    snprintf(tmp, STRMAXLEN(tmp), "%u", res);
    string_buffer_append(buf, tmp);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t res = *(int64_t *)raw;
    snprintf(tmp, STRMAXLEN(tmp), "%ld", res);
    string_buffer_append(buf, tmp);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t res = *(uint64_t *)raw;
    snprintf(tmp, STRMAXLEN(tmp), "%lu", res);
    string_buffer_append(buf, tmp);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t res = *(uint8_t *)raw;
    snprintf(tmp, STRMAXLEN(tmp), "%c", res);
    string_buffer_append(buf, tmp);
  } break;
  case X86_64_TYPE_STRING: {
    uint8_t *res = (uint8_t *)raw;
    string_buffer_append(buf, "\"");
    string_buffer_append(buf, (char *)res);
    string_buffer_append(buf, "\"");
  } break;
  case X86_64_TYPE_ARRAY: {
    string_buffer_append(buf, "[");
    uint64_t i = 0;
    if (i < entry->meta_array->count) {
      uint8_t *raw_cur = raw + entry->meta_array->size * i;
      cmd_data_str_raw(buf, type, entry->i_child, raw_cur);
      ++i;
    }
    for (; i < entry->meta_array->count; ++i) {
      string_buffer_append(buf, ", ");
      uint8_t *raw_cur = raw + entry->meta_array->size * i;
      cmd_data_str_raw(buf, type, entry->i_child, raw_cur);
    }
    string_buffer_append(buf, "]");
  } break;
  case X86_64_TYPE_OBJECT: {
  default:
    string_buffer_append(buf, "<unknown>");
  } break;
  }
}

static void cmd_data_str(x86_64_data *data, char **type_p, char **value_p) {
  if (!data) {
    if (value_p) {
      *value_p = strdup("()");
    }
    if (type_p) {
      *type_p = strdup("void");
    }
    return;
  }
  string_buffer *buf;

  if (value_p) {
    buf = string_buffer_new(32, 0);
    cmd_data_str_raw(buf, data->type, 0, data->raw);
    *value_p = string_buffer_detach(buf);
  }

  if (type_p) {
    buf = string_buffer_new(32, 0);
    cmd_data_str_type(buf, data->type, 0);
    *type_p = string_buffer_detach(buf);
  }
}

static void cmd_out_value_str(context *ctx, const char *prefix,
                              const char *name, cmd_value_str value) {
  context_out_append(ctx, prefix);
  context_out_append(ctx, name);
  if (value.type) {
    context_out_append(ctx, ": ");
    context_out_append(ctx, value.type);
    free(value.type);
  }
  context_out_append(ctx, " = ");
  if (value.value) {
    context_out_append(ctx, value.value);
    free(value.value);
  }
  context_out_appendln(ctx, "");
}

static int cmd_var_arg(context *ctx, const char *rest) {
  const char             *args = rest;
  struct user_regs_struct regs;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    return -1;
  }

  if (context_tg_getregs(ctx, &regs) == TARGET_ERROR) {
    context_error(ctx, "failed to get regs");
    return -1;
  }

  // get current subroutine based on rsp
  debug_sub *sub = context_dbg_sub_by_address(ctx, regs.rip);
  if (!sub) {
    context_error(ctx, "no debug info about current subroutine");
    return -1;
  }

  if (sub->address_prologue_end > regs.rip) {
    context_error(ctx, "can't get info while in subroutine prologue");
    return -1;
  }

  if (sub->address_epilogue_start < regs.rip) {
    context_error(ctx, "can't get info while in subroutine epilogue");
    return -1;
  }

  int found = 0;

  for (uint64_t i = 0; i < sub->args_cnt; ++i) {
    const debug_arg *arg = sub->args + i;
    const char      *arg_name = context_dbg_str_by_id(ctx, arg->strs_index);
    if (!strcmp(arg_name, args)) {
      uint64_t      address = regs.rbp + arg->rbp_offset;
      cmd_value_str value = cmd_print_value_str(ctx, address);
      cmd_out_value_str(ctx, "arg ", arg_name, value);
      found = 1;
    }
  }

  for (uint64_t i = 0; i < sub->vars_cnt; ++i) {
    const debug_var *var = sub->vars + i;
    const char      *var_name = context_dbg_str_by_id(ctx, var->strs_index);
    if (!strcmp(var_name, args)) {
      uint64_t      address = regs.rbp + var->rbp_offset;
      cmd_value_str value = cmd_print_value_str(ctx, address);
      cmd_out_value_str(ctx, "arg ", var_name, value);
      found = 1;
    }
  }

  if (!found) {
    context_err_append(ctx, "in subroutine ");
    context_err_append(ctx, context_dbg_str_by_id(ctx, sub->strs_index));
    context_error(ctx, " arg or var not found");
    return -1;
  }

  return 0;
}

static int fn_register(context *ctx, const char *rest) {
  char                    buf[64];
  struct user_regs_struct regs;
  const char             *reg = rest + 1;

  if (cmd_error_if_no_target(ctx) || cmd_error_if_not_running(ctx)) {
    return -1;
  }

  if (context_tg_getregs(ctx, &regs) == TARGET_ERROR) {
    context_error(ctx, "failed to get regs");
    return -1;
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
    ctx->s_debugger = DEBUGGER_ERROR;
    context_err_append(ctx, "unknown register ");
    context_err_appendln(ctx, reg);
    return -1;
  }

  snprintf(buf, STRMAXLEN(buf), "%#llx",
           *(unsigned long long *)(((void *)&regs) + offset));
  context_out_appendln(ctx, buf);
  return 0;
}

static void fn(cmd_handler *handler, context *ctx, const char *rest) {
  UNUSED(handler);

  char *args = trim(strdup(rest));

  if (!args || !*args) {
    goto error_format;
  }

  if (args[0] == '$') {
    fn_register(ctx, args);
  } else {
    cmd_var_arg(ctx, args);
  }
  goto cleanup;

error_format:
  cmd_error_format(ctx, args, help);

cleanup:
  if (args) {
    free(args);
  }
}

cmd_value_str cmd_print_value_str(context *ctx, uint64_t address) {
  cmd_value_str result = {.type = NULL, .value = NULL};

  x86_64_value value = {.data = cmd_peek_data(ctx, address)};
  cmd_data_str(value.data, &result.type, &result.value);
  __x86_64_value_drop(&value);

  return result;
}

cmd *cmd_print(const char *cmd_ref) {
  return cmd_entry_new(fn, desc, help, cmd_ref);
}
