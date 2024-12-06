#include "util/log.h"
#include "util/macro.h"
#include "x86_64_ctx.h"

#include "x86_64_core/x86_64_core.h"
#include "x86_64_core/x86_64_meta.h"

#include <stdio.h>
#include <string.h>

static void cg_op_push_back(list_cg_x86_64_op *operands,
                            cg_x86_64_op      *operand) {
  list_cg_x86_64_op_push_back(operands, operand);
}

static cg_x86_64_op *cg_mode_new_direct_symbol(x86_64_symbol_enum kind) {
  return cg_x86_64_op_new_direct(strdup(x86_64_symbol_enum_str(kind)));
}

static size_t cg_with_align(size_t size, size_t align) {
  return ((size - 1) / align + 1) * align;
}

static void cg_units_append_move(list_cg_x86_64_unit *to,
                                 list_cg_x86_64_unit *from) {
  for (list_cg_x86_64_unit_it it = list_cg_x86_64_unit_begin(from);
       !it.end(&it); it.next(&it)) {
    list_cg_x86_64_unit_push_back(to, it.get(&it));
  }
  while (!list_cg_x86_64_unit_empty(from)) {
    list_cg_x86_64_unit_pop_front(from);
  }
  list_cg_x86_64_unit_free(from);
}

static int ctx_is_exit(cg_x86_64_ctx *ctx, const mir_subroutine *sub) {
  UNUSED(ctx);
  return !strcmp(sub->name, "main");
}

static int ctx_is_global(cg_x86_64_ctx *ctx, const mir_node_base *base) {
  hashset_cg_x86_64_mir_ref_it it =
      hashset_cg_x86_64_mir_ref_find(ctx->globls, (mir_node_base *)base);
  return !it.end(&it);
}

static char *ctx_local_name(cg_x86_64_ctx *ctx, const char *old) {
  UNUSED(ctx);
  size_t len = strlen(old);
  char  *name = malloc(len + 3);
  memcpy(name, ".L", 2);
  memcpy(name + 2, old, len + 1);
  return name;
}

__attribute__((__unused__)) static void ctx_data_push_back(cg_x86_64_ctx *ctx,
                                                           void *data) {
  list_cg_x86_64_unit_push_back(ctx->data_units_ref, data);
}

__attribute__((__unused__)) static void ctx_ext_push_back(cg_x86_64_ctx *ctx,
                                                          void          *data) {
  list_cg_x86_64_unit_push_back(ctx->ext_units_ref, data);
}

__attribute__((__unused__)) static void ctx_text_push_back(cg_x86_64_ctx *ctx,
                                                           void *text) {
  list_cg_x86_64_unit_push_back(ctx->text_units_ref, text);
}

static cg_x86_64_map_value *ctx_value_find(cg_x86_64_ctx *ctx,
                                           mir_value     *value) {
  hashset_cg_x86_64_map_value_it it;

  it = hashset_cg_x86_64_map_value_find(
      ctx->sub_map_values,
      &(cg_x86_64_map_value){.value_ref = (mir_value *)value});

  if (it.end(&it)) {
    error("value %p is not in sub_map_values", value);
    return NULL;
  }

  return it.get(&it);
}

static void ctx_sub_prologue(cg_x86_64_ctx *ctx) {
  list_cg_x86_64_op *operands;

  // pushq %rbp
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_PUSHQ, operands));
  // movq %rsp, %rbp
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

  ctx->sub_stack_size += X86_64_ALIGN_REG_64;
  ctx->sub_rbp_offset = ctx->sub_stack_size;
}

static char *ctx_sub_epilogue_name(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
  const char epilogue[] = "_ep";

  size_t ssz = strlen(sub->name);
  size_t lsz = ssz + sizeof(epilogue);
  char  *label = malloc(lsz);
  memcpy(label, sub->name, ssz);
  memcpy(label + ssz, epilogue, sizeof(epilogue));
  char *local = ctx_local_name(ctx, label);
  free(label);
  return local;
}

static void ctx_sub_epilogue(cg_x86_64_ctx *ctx, mir_subroutine *sub, int ret) {
  ctx_text_push_back(ctx, cg_x86_64_label_new(ctx_sub_epilogue_name(ctx, sub),
                                              CG_X86_64_LABEL_INSTR));

  list_cg_x86_64_op *operands;
  // movq %rsp, %rbp
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  // popq %rbp
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));

  ctx->sub_stack_size -= X86_64_ALIGN_REG_64;
  ctx->sub_rbp_offset = 0;

  // ret
  if (ret) {
    operands = list_cg_x86_64_op_new();
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_RETQ, operands));
    ctx->sub_stack_size -= X86_64_ALIGN_REG_64;
  }
}

static void ctx_sub_exit(cg_x86_64_ctx *ctx) {
  list_cg_x86_64_op *operands;

  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_immediate(60));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_XORQ, operands));

  operands = list_cg_x86_64_op_new();
  ctx_text_push_back(ctx,
                     cg_x86_64_instr_new(CG_X86_64_MNEM_SYSCALL, operands));
}

static void ctx_value_prologue(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
  list_cg_x86_64_op *operands;

  { // reserve memory on stack
    size_t values = 0;

    values += sub->ret ? 1 : 0;
    values += list_mir_value_size(sub->args);
    values += list_mir_value_size(sub->vars);
    values += list_mir_value_size(sub->tmps);

    size_t rsp_sub =
        cg_with_align(values * sizeof(x86_64_value), X86_64_ALIGN_CALL);

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(rsp_sub));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_SUBQ, operands));

    ctx->sub_stack_size += rsp_sub;
  }

  // index values based on rbp
  size_t rbp_offset = 0;
  if (sub->ret) {
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(sub->ret, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->args); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->vars); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->tmps); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }
}

static char *ctx_block_name(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                            mir_bb *bb) {
  size_t bb_sz = strlen(sub->name) + 1 + 32;
  char  *bb_name = malloc(bb_sz);
  snprintf(bb_name, bb_sz, "%s_%zu", sub->name, bb->id);
  char *local = ctx_local_name(ctx, bb_name);
  free(bb_name);
  return local;
}

static void ctx_stmt_store_assign(cg_x86_64_ctx *ctx, mir_value *lsv,
                                  mir_value *rsv) {
  cg_x86_64_map_value *cg_lsv, *cg_rsv;
  list_cg_x86_64_op   *operands;

  cg_lsv = ctx_value_find(ctx, lsv);
  cg_rsv = ctx_value_find(ctx, rsv);

  // leaq lsv_off(%rbp), %rdi
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg_lsv->rbp_offset, CG_X86_64_REG_RBP));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ, operands));

  // movq rsv_off(%rbp), %rsi
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg_rsv->rbp_offset, CG_X86_64_REG_RBP));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

  // call __x86_64_assign
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_ASSIGN));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
}

static void ctx_value_drop(cg_x86_64_ctx *ctx, cg_x86_64_map_value *cg_lsv) {
  list_cg_x86_64_op *operands;

  // leaq lsv_off(%rbp), rdi
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg_lsv->rbp_offset, CG_X86_64_REG_RBP));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ, operands));

  // call __x86_64_drop
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands,
                  cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_DROP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
}

static char *ctx_data_name(cg_x86_64_ctx *self, mir_subroutine *sub,
                           mir_value *value) {
  char buf[32];

  snprintf(buf, STRMAXLEN(buf), "_data_%lu", value->id);
  size_t d_sz = strlen(sub->name) + strlen(buf) + 1;
  char  *name = malloc(d_sz);
  {
    char *end = name;
    end = stpcpy(end, sub->name);
    end = stpcpy(end, buf);
  }
  char *local = ctx_local_name(self, name);
  free(name);
  return local;
}

static void ctx_stmt_store_lit(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                               mir_value *lsv, mir_lit *rsv) {
  list_cg_x86_64_op   *operands;
  cg_x86_64_map_value *cg_lsv = ctx_value_find(ctx, lsv);

  type_enum rsv_type = rsv->lit->type->type;

  // init
  if (rsv_type == TYPE_VOID) {
    // leaq lsv_off(%rbp), rdi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(
        operands, cg_x86_64_op_new_base(cg_lsv->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ, operands));

    // call __x86_64_init
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INIT));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
    return;
  }

  ctx_value_drop(ctx, cg_lsv);

  switch (rsv_type) {
  case TYPE_BOOL: {
    uint8_t value = rsv->lit->value.v_bool;

    // movb $bool, %dil
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB, operands));

    // call __x86_64_value_bool
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BOOL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  } break;
  case TYPE_BYTE: {
    uint8_t value = rsv->lit->value.v_ulong;

    // movb $byte, %dil
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB, operands));

    // call __x86_64_value_byte
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BYTE));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  } break;
  case TYPE_INT: {
    int32_t value = rsv->lit->value.v_long;

    // movb $int, %edi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL, operands));

    // call __x86_64_value_int
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INT));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  } break;
  case TYPE_UINT: {
    uint32_t value = rsv->lit->value.v_ulong;

    // movb $uint, %edi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL, operands));

    // call __x86_64_value_uint
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_UINT));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_LONG: {
    int64_t value = rsv->lit->value.v_long;

    // movb $long, %rdi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    // call __x86_64_value_long
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_LONG));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_ULONG: {
    uint64_t value = rsv->lit->value.v_ulong;

    // movb $ulong, %rdi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    // call __x86_64_value_ulong
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ULONG));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  } break;
  case TYPE_CHAR: {
    char   *chr = rsv->lit->value.v_char;
    uint8_t value = chr[0];

    // movb $char, %dil
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB, operands));

    // call __x86_64_value_char
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_CHAR));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  } break;
  case TYPE_STRING: {
    char *str = strdup(rsv->lit->value.v_str);

    // add to data region
    ctx_data_push_back(ctx, cg_x86_64_label_new(ctx_data_name(ctx, sub, lsv),
                                                CG_X86_64_LABEL_DATA));
    ctx_data_push_back(ctx, cg_x86_64_data_new_ascii((uint8_t *)str));

    // movb data_label, %rdi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_direct(ctx_data_name(ctx, sub, lsv)));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ, operands));

    // call __x86_64_value_string
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_STRING));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  } break;
  default:
    error("unexpected lit type %s %p", type_enum_str(rsv_type), rsv);
    return;
  }

  // store value to stack
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg_lsv->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
}

void ctx_stmt_store_builtin(cg_x86_64_ctx *ctx, mir_value *lsv,
                            mir_builtin *rsv) {}

void ctx_stmt_store_to_builtin(cg_x86_64_ctx *ctx, mir_builtin *lsv,
                               mir_value *rsv) {}

static void ctx_stmt_store(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                           mir_builtin *stmt) {
  if (list_mir_arg_size(stmt->args) != 2) {
    error("store requires 2 args");
    goto error;
  }

  mir_node_base *first;
  mir_node_base *second;

  {
    list_mir_arg_it it = list_mir_arg_begin(stmt->args);
    first = it.get(&it);
    it.next(&it);
    second = it.get(&it);
  }

  switch (first->kind) {
  case MIR_NODE_VALUE: {
    switch (second->kind) {
    case MIR_NODE_VALUE: {
      ctx_stmt_store_assign(ctx, (mir_value *)first, (mir_value *)second);
    } break;
    case MIR_NODE_LIT: {
      ctx_stmt_store_lit(ctx, sub, (mir_value *)first, (mir_lit *)second);
    } break;
    case MIR_NODE_BUILTIN: {
      ctx_stmt_store_builtin(ctx, (mir_value *)first, (mir_builtin *)second);
    } break;
    default:
      goto unhandled;
    }
  } break;
  case MIR_NODE_BUILTIN: {
    switch (second->kind) {
    case MIR_NODE_VALUE:
      ctx_stmt_store_to_builtin(ctx, (mir_builtin *)first, (mir_value *)second);
      break;
    default:
      goto unhandled;
    }
  } break;
  default:
    goto unhandled;
  }

  return;

unhandled:
  error("unexpected pair %d %d", first->kind, second->kind);
error:
  return;
}

static void ctx_stmt_drop(cg_x86_64_ctx *ctx, mir_builtin *stmt) {
  union {
    mir_node_base *node;
    mir_value     *value;
  } lsv;

  if (list_mir_arg_size(stmt->args) != 1) {
    error("expected 1 arg for drop statement");
    return;
  }

  lsv.node = (mir_node_base *)list_mir_arg_front(stmt->args);

  if (lsv.node->kind != MIR_NODE_VALUE) {
    error("arg should be value, got %d %p", lsv.node->kind, lsv.node);
    return;
  }

  cg_x86_64_map_value *cg_lsv = ctx_value_find(ctx, lsv.value);
  ctx_value_drop(ctx, cg_lsv);
}

// no jump only on last block
static void ctx_jump(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_jmp *jmp) {
  list_cg_x86_64_op *operands;

  if (!jmp) {
    return;
  }

  // conditional
  if (jmp->value_ref) {
    cg_x86_64_map_value *cg_lsv = ctx_value_find(ctx, jmp->value_ref);

    // movq lsv_off(%rbp), rdi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(
        operands, cg_x86_64_op_new_base(cg_lsv->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    // call __x86_64_unwrap_bool
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_BOOL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

    // textb %al, %al
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_AL));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_AL));
    ctx_text_push_back(ctx,
                       cg_x86_64_instr_new(CG_X86_64_MNEM_TESTB, operands));

    // je .Lje
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_direct(
                                  ctx_block_name(ctx, sub, jmp->je_ref)));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_JE, operands));
  }

  // jmp .Ljz
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(
      operands, cg_x86_64_op_new_direct(ctx_block_name(ctx, sub, jmp->jz_ref)));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_JMP, operands));
}

static void ctx_stmt(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                     mir_node_base *stmt) {
  switch (stmt->kind) {
  case MIR_NODE_BUILTIN: {
    mir_builtin *builtin = (mir_builtin *)stmt;
    switch (builtin->kind) {
    case MIR_BUILTIN_STORE: {
      ctx_stmt_store(ctx, sub, builtin);
    } break;
    case MIR_BUILTIN_DROP: {
      ctx_stmt_drop(ctx, builtin);
    } break;
    default:
      error("unexpected builtin kind %d of stmt %p", builtin->kind, stmt);
    }
  } break;
  default:
    error("unexpected kind %d of stmt %p", stmt->kind, stmt);
    break;
  }
}

static void ctx_block(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_bb *bb) {
  ctx_text_push_back(ctx, cg_x86_64_label_new(ctx_block_name(ctx, sub, bb),
                                              CG_X86_64_LABEL_INSTR));

  for (list_mir_node_it it = list_mir_node_begin(bb->stmts); !it.end(&it);
       it.next(&it)) {
    mir_node_base *stmt = it.get(&it);
    ctx_stmt(ctx, sub, stmt);
  }

  ctx_jump(ctx, sub, bb->jmp);
}

static char *ctx_sub_name(cg_x86_64_ctx *ctx, mir_subroutine *sub, int global) {
  if (global) {
    return strdup(sub->name);
  } else {
    return ctx_local_name(ctx, sub->name);
  }
}

static void ctx_sub(cg_x86_64_ctx *ctx, mir_subroutine *sub, int exit,
                    int global) {
  ctx->sub_stack_size = X86_64_ALIGN_REG_64;
  ctx->sub_map_values = hashset_cg_x86_64_map_value_new();

  ctx_text_push_back(ctx, cg_x86_64_label_new(ctx_sub_name(ctx, sub, global),
                                              CG_X86_64_LABEL_INSTR));

  ctx_sub_prologue(ctx);
  ctx_value_prologue(ctx, sub);

  for (list_mir_bb_it it = list_mir_bb_begin(sub->bbs); !it.end(&it);
       it.next(&it)) {
    ctx_block(ctx, sub, it.get(&it));
  }

  ctx_sub_epilogue(ctx, sub, !exit);
  if (exit) {
    ctx_sub_exit(ctx);
  }

  hashset_cg_x86_64_map_value_free(ctx->sub_map_values);
}

void cg_x86_64_ctx_build(cg_x86_64_ctx *ctx, mir *mir) {
  list_cg_x86_64_unit *units = ctx->cg_ref->units;

  cg_x86_64_dir *section_data = cg_x86_64_dir_section_new(strdup(".data"));
  list_cg_x86_64_unit *data_units = list_cg_x86_64_unit_new();
  ctx->data_units_ref = data_units;

  cg_x86_64_dir *section_text = cg_x86_64_dir_section_new(strdup(".text"));

  list_cg_x86_64_unit *globl_units = list_cg_x86_64_unit_new();

  list_cg_x86_64_unit *ext_units = list_cg_x86_64_unit_new();
  ctx->ext_units_ref = ext_units;

  ctx->globls = hashset_cg_x86_64_mir_ref_new();

  // core declarations
  for (int kind = 0; kind < X86_64_SYMBOL_UNKNOWN; ++kind) {
    list_cg_x86_64_unit_push_back(
        ext_units,
        (cg_x86_64_unit *)cg_x86_64_dir_extern_new(cg_x86_64_label_new(
            strdup(x86_64_symbol_enum_str(kind)), CG_X86_64_LABEL_DIRECTIVE)));
  }

  // executable declarations
  for (list_mir_subroutine_ext_it it =
           list_mir_subroutine_ext_begin(mir->sub_exts);
       !it.end(&it); it.next(&it)) {
    mir_subroutine_ext *ext = it.get(&it);
    char               *ext_name = strdup(ext->name);

    list_cg_x86_64_unit_push_back(
        ext_units,
        (cg_x86_64_unit *)cg_x86_64_dir_extern_new(
            cg_x86_64_label_new(ext_name, CG_X86_64_LABEL_DIRECTIVE)));

    hashset_cg_x86_64_mir_ref_insert(ctx->globls, (mir_node_base *)ext);
  }

  // add to globals
  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
       !it.end(&it); it.next(&it)) {
    mir_subroutine *sub = it.get(&it);

    if (!strcmp(sub->name, "main")) {
      list_cg_x86_64_unit_push_back(
          globl_units,
          (cg_x86_64_unit *)cg_x86_64_dir_globl_new(cg_x86_64_label_new(
              strdup(sub->name), CG_X86_64_LABEL_DIRECTIVE)));

      hashset_cg_x86_64_mir_ref_insert(ctx->globls, (mir_node_base *)sub);
    }
  }

  list_cg_x86_64_unit *text_units = list_cg_x86_64_unit_new();
  ctx->text_units_ref = text_units;

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
       !it.end(&it); it.next(&it)) {
    mir_subroutine *sub = it.get(&it);

    int exit = ctx_is_exit(ctx, sub);
    int global = ctx_is_global(ctx, (mir_node_base *)sub);

    ctx_sub(ctx, sub, exit, global);
  }

  // .section .data
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)section_data);
  cg_units_append_move(units, data_units);

  // .section .text
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)section_text);
  cg_units_append_move(units, ext_units);
  cg_units_append_move(units, globl_units);
  cg_units_append_move(units, text_units);

  hashset_cg_x86_64_mir_ref_free(ctx->globls);
}
