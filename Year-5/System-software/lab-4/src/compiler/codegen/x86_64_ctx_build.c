#include "x86_64_ctx.h"

#include "util/log.h"
#include "util/macro.h"
#include "x86_64_core/meta.h"
#include "x86_64_core/type.h"
#include "x86_64_core/value.h"

#include <stdio.h>
#include <string.h>

#define cg_mir_node_kind_assign(node, node_kind, assign, assign_kind)          \
  do {                                                                         \
    if (node_kind != assign_kind) {                                            \
      error("expected arg %d, got %d %p", assign_kind, node_kind, node);       \
      return;                                                                  \
    }                                                                          \
    assign = (void *)node;                                                     \
  } while (0)

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
  while (!list_cg_x86_64_unit_empty(from)) {
    list_cg_x86_64_unit_push_back(to, list_cg_x86_64_unit_pop_front(from));
  }
  list_cg_x86_64_unit_free(from);
}

static x86_64_type *cg_type_to_x86_64(type_base *type) {
  switch (type->type) {
  case TYPE_BOOL:
    return x86_64_type_new_simple(X86_64_TYPE_BOOL);
    break;
  case TYPE_BYTE:
    return x86_64_type_new_simple(X86_64_TYPE_BYTE);
    break;
  case TYPE_INT:
    return x86_64_type_new_simple(X86_64_TYPE_INT);
    break;
  case TYPE_UINT:
    return x86_64_type_new_simple(X86_64_TYPE_UINT);
    break;
  case TYPE_LONG:
    return x86_64_type_new_simple(X86_64_TYPE_LONG);
    break;
  case TYPE_ULONG:
    return x86_64_type_new_simple(X86_64_TYPE_ULONG);
    break;
  case TYPE_CHAR:
    return x86_64_type_new_simple(X86_64_TYPE_CHAR);
    break;
  case TYPE_STRING:
    return x86_64_type_new_simple(X86_64_TYPE_STRING);
    break;
  case TYPE_VOID:
    return x86_64_type_new_simple(X86_64_TYPE_VOID);
    break;
  // NOTE: metadata is NULL
  // TODO: don't recreate after each wrap
  case TYPE_ARRAY: {
    x86_64_type *elem_type = cg_type_to_x86_64(((type_array *)type)->elem_type);
    x86_64_type *complex =
        x86_64_type_new_complex(X86_64_TYPE_ARRAY, elem_type, NULL);
    x86_64_type_free(elem_type);
    return complex;
  }
  case TYPE_CUSTOM:
  case TYPE_CALLABLE:
    error("type %d is not implemented, %p", type->type, type);
    return NULL;
  default:
    error("unknown type %d %p", type->type, type);
    return NULL;
  }
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
  list_cg_x86_64_unit_push_back(ctx->data_units_ref, (cg_x86_64_unit *)data);
}

__attribute__((__unused__)) static void ctx_ext_push_back(cg_x86_64_ctx *ctx,
                                                          void          *data) {
  list_cg_x86_64_unit_push_back(ctx->ext_units_ref, (cg_x86_64_unit *)data);
}

__attribute__((__unused__)) static void ctx_text_push_back(cg_x86_64_ctx *ctx,
                                                           void *text) {
  list_cg_x86_64_unit_push_back(ctx->text_units_ref, (cg_x86_64_unit *)text);
}

__attribute__((__unused__)) static void
ctx_text_push_back_many(cg_x86_64_ctx *ctx, list_cg_x86_64_unit *other) {
  cg_units_append_move(ctx->text_units_ref, other);
}

static void ctx_type_serialize(cg_x86_64_ctx *ctx, x86_64_type *type) {
  if (!type) {
    return;
  }
  size_t   sz = sizeof(x86_64_type) + sizeof(type->entries[0]) * type->count;
  uint8_t *data = MALLOCN(uint8_t, sz);
  memcpy(data, type, sz);
  ctx_data_push_back(ctx, cg_x86_64_data_new_bytes(sz, data));
}

static void ctx_stack_release(cg_x86_64_ctx *ctx, size_t stack_size) {
  list_cg_x86_64_op *operands;
  if (stack_size) {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(stack_size));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_ADDQ, operands));
    ctx->sub_stack_size -= stack_size;
  }
}

static cg_x86_64_map_value *ctx_sub_value_find(cg_x86_64_ctx *ctx,
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
  list_cg_x86_64_op *operands;

  ctx_text_push_back(ctx, cg_x86_64_label_new(ctx_sub_epilogue_name(ctx, sub),
                                              CG_X86_64_LABEL_INSTR));

  // set rax to return value
  if (sub->ret) {
    operands = list_cg_x86_64_op_new();
    cg_x86_64_map_value *cg_ret = ctx_sub_value_find(ctx, sub->ret);
    cg_op_push_back(
        operands, cg_x86_64_op_new_base(cg_ret->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  }

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
  cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_FFLUSH));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

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

static void ctx_sub_values_prologue(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
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
    rbp_offset -= sizeof(x86_64_value);
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(sub->ret, rbp_offset));
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->args); !it.end(&it);
       it.next(&it)) {
    rbp_offset -= sizeof(x86_64_value);
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->vars); !it.end(&it);
       it.next(&it)) {
    rbp_offset -= sizeof(x86_64_value);
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->tmps); !it.end(&it);
       it.next(&it)) {
    rbp_offset -= sizeof(x86_64_value);
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
  }
}

// copy args to allocated stack
static void ctx_sub_args_prologue(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
  list_cg_x86_64_op   *operands;
  cg_x86_64_map_value *cg;
  uint64_t             offset = ctx->sub_rbp_offset;

  list_mir_value_it it = list_mir_value_begin(sub->args);

  if (it.end(&it)) {
    return;
  }
  cg = ctx_sub_value_find(ctx, it.get(&it));
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  it.next(&it);

  if (it.end(&it)) {
    return;
  }
  cg = ctx_sub_value_find(ctx, it.get(&it));
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  it.next(&it);

  if (it.end(&it)) {
    return;
  }
  cg = ctx_sub_value_find(ctx, it.get(&it));
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  it.next(&it);

  if (it.end(&it)) {
    return;
  }
  cg = ctx_sub_value_find(ctx, it.get(&it));
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  it.next(&it);

  if (it.end(&it)) {
    return;
  }
  cg = ctx_sub_value_find(ctx, it.get(&it));
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  it.next(&it);

  if (it.end(&it)) {
    return;
  }
  cg = ctx_sub_value_find(ctx, it.get(&it));
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  it.next(&it);

  while (!it.end(&it)) {
    cg = ctx_sub_value_find(ctx, it.get(&it));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_base(offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
    cg_op_push_back(operands,
                    cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
    it.next(&it);
    offset += sizeof(x86_64_value);
  }
}

static void ctx_sub_value_drop(cg_x86_64_ctx       *ctx,
                               cg_x86_64_map_value *cg_lsv) {
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

static char *ctx_block_name(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                            mir_bb *bb) {
  size_t bb_sz = strlen(sub->name) + 1 + 32;
  char  *bb_name = malloc(bb_sz);
  snprintf(bb_name, bb_sz, "%s_%zu", sub->name, bb->id);
  char *local = ctx_local_name(ctx, bb_name);
  free(bb_name);
  return local;
}

static char *ctx_data_name(cg_x86_64_ctx *ctx, mir_subroutine *sub,
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
  char *local = ctx_local_name(ctx, name);
  free(name);
  return local;
}

static char *ctx_sub_name(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
  if (ctx_is_global(ctx, (mir_node_base *)sub)) {
    return strdup(sub->name);
  } else {
    return ctx_local_name(ctx, sub->name);
  }
}

static char *ctx_ext_name(cg_x86_64_ctx *ctx, mir_subroutine_ext *ext) {
  if (ctx_is_global(ctx, (mir_node_base *)ext)) {
    return strdup(ext->name);
  } else {
    return ctx_local_name(ctx, ext->name);
  }
}

static uint64_t ctx_args_value_prepare(cg_x86_64_ctx *ctx, int variadic,
                                       const list_cg_x86_64_map_value *values,
                                       int64_t arg_offset) {
  list_cg_x86_64_op          *operands;
  cg_x86_64_map_value        *cg;
  size_t                      aligned_rsp_size = 0;
  size_t                      align;
  size_t                      prev_stack_size = ctx->sub_stack_size;
  list_cg_x86_64_unit        *texts = list_cg_x86_64_unit_new();
  list_cg_x86_64_map_value_it it = list_cg_x86_64_map_value_begin(values);
  list_cg_x86_64_op          *v_ops;

  if (arg_offset < 0) {
    arg_offset = 0;
  }

  if (variadic) {
    v_ops = list_cg_x86_64_op_new();
    cg_op_push_back(v_ops, cg_x86_64_op_new_immediate(0));
  }

  switch (arg_offset) {
  case 0:
    if (it.end(&it)) {
      if (variadic) {
        cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
        list_cg_x86_64_unit_push_back(
            texts,
            (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, v_ops));
      }
      variadic = 0;
      break;
    }
    cg = it.get(&it);
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                             CG_X86_64_MNEM_MOVQ, operands));
    it.next(&it);
    __attribute__((fallthrough));
  case 1:
    if (it.end(&it)) {
      if (variadic) {
        cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
        list_cg_x86_64_unit_push_back(
            texts,
            (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, v_ops));
      }
      variadic = 0;
      break;
    }
    cg = it.get(&it);
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
    list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                             CG_X86_64_MNEM_MOVQ, operands));
    it.next(&it);
    __attribute__((fallthrough));
  case 2:
    if (it.end(&it)) {
      if (variadic) {
        cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
        list_cg_x86_64_unit_push_back(
            texts,
            (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, v_ops));
      }
      variadic = 0;
      break;
    }
    cg = it.get(&it);
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
    list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                             CG_X86_64_MNEM_MOVQ, operands));
    it.next(&it);
    __attribute__((fallthrough));
  case 3:
    if (it.end(&it)) {
      if (variadic) {
        cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
        list_cg_x86_64_unit_push_back(
            texts,
            (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, v_ops));
      }
      variadic = 0;
      break;
    }
    cg = it.get(&it);
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
    list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                             CG_X86_64_MNEM_MOVQ, operands));
    it.next(&it);
    __attribute__((fallthrough));
  case 4:
    if (it.end(&it)) {
      if (variadic) {
        cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
        list_cg_x86_64_unit_push_back(
            texts,
            (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, v_ops));
      }
      variadic = 0;
      break;
    }
    cg = it.get(&it);
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
    list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                             CG_X86_64_MNEM_MOVQ, operands));
    it.next(&it);
    __attribute__((fallthrough));
  case 5:
    if (it.end(&it)) {
      if (variadic) {
        cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
        list_cg_x86_64_unit_push_back(
            texts,
            (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, v_ops));
      }
      variadic = 0;
      break;
    }
    cg = it.get(&it);
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
    list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                             CG_X86_64_MNEM_MOVQ, operands));
    it.next(&it);
    __attribute__((fallthrough));
  case 6:
    while (!it.end(&it)) {
      cg = it.get(&it);
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands,
                      cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
      list_cg_x86_64_unit_push_front(
          texts, (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_PUSHQ,
                                                       operands));
      ctx->sub_stack_size += sizeof(x86_64_value);
      it.next(&it);
    }
    __attribute__((fallthrough));
  default:
    break;
  }

  if (variadic) {
    list_cg_x86_64_unit_push_front(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                              CG_X86_64_MNEM_PUSHQ, operands));
    ctx->sub_stack_size += sizeof(x86_64_value);
  }

  // stack is pushed by who executes this command, so no need to no anything
  aligned_rsp_size = cg_with_align(ctx->sub_stack_size, X86_64_ALIGN_CALL);
  align = aligned_rsp_size - ctx->sub_stack_size;

  if (align) {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(align));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
    list_cg_x86_64_unit_push_front(texts, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                              CG_X86_64_MNEM_SUBQ, operands));
    ctx->sub_stack_size += align;
  }

  ctx_text_push_back_many(ctx, texts);

  return ctx->sub_stack_size - prev_stack_size;
}

static void ctx_stmt_store_lit(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                               mir_value *lsv, mir_lit *rsv) {
  list_cg_x86_64_op   *operands;
  cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, lsv);

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

  ctx_sub_value_drop(ctx, cg_lsv);

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

static void ctx_stmt_store_assign(cg_x86_64_ctx *ctx, mir_value *lsv,
                                  mir_value *rsv) {
  cg_x86_64_map_value *cg_lsv, *cg_rsv;
  list_cg_x86_64_op   *operands;

  cg_lsv = ctx_sub_value_find(ctx, lsv);
  cg_rsv = ctx_sub_value_find(ctx, rsv);

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

static void ctx_stmt_builtin_l_value(cg_x86_64_ctx     *ctx,
                                     x86_64_symbol_enum symbol, int variadic,
                                     const list_cg_x86_64_map_value *values) {
  list_cg_x86_64_op *operands;
  size_t             stack_size;

  stack_size = ctx_args_value_prepare(ctx, variadic, values, 0);

  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_mode_new_direct_symbol(symbol));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  ctx_stack_release(ctx, stack_size);
}

static void ctx_stmt_builtin_f_value(cg_x86_64_ctx     *ctx,
                                     x86_64_symbol_enum symbol, int variadic,
                                     size_t n, ...) {
  va_list args;
  va_start(args, n);

  list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();
  while (n--) {
    list_cg_x86_64_map_value_push_back(values,
                                       va_arg(args, cg_x86_64_map_value *));
  }
  ctx_stmt_builtin_l_value(ctx, symbol, variadic, values);

  while (!list_cg_x86_64_map_value_empty(values)) {
    list_cg_x86_64_map_value_pop_front(values);
  }
  list_cg_x86_64_map_value_free(values);
}

static void ctx_stmt_builtin_2(cg_x86_64_ctx *ctx, mir_builtin *builtin,
                               x86_64_symbol_enum symbol) {
  if (list_mir_arg_size(builtin->args) != 2) {
    error("expected 2 args %p", builtin);
    return;
  }

  union {
    mir_node_base       *node;
    mir_value           *value;
    cg_x86_64_map_value *cg;
  } a1, a2;

  list_mir_arg_it it = list_mir_arg_begin(builtin->args);
  a1.node = it.get(&it);
  it.next(&it);
  a2.node = it.get(&it);

  cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);
  cg_mir_node_kind_assign(a2.node, a2.node->kind, a2.value, MIR_NODE_VALUE);

  a1.cg = ctx_sub_value_find(ctx, a1.value);
  a2.cg = ctx_sub_value_find(ctx, a2.value);

  ctx_stmt_builtin_f_value(ctx, symbol, 0, 2, a1.cg, a2.cg);
}

void ctx_stmt_builtin_1(cg_x86_64_ctx *ctx, mir_builtin *builtin,
                        x86_64_symbol_enum symbol) {
  if (list_mir_arg_size(builtin->args) != 1) {
    error("expected 1 args %p", builtin);
    return;
  }

  union {
    mir_node_base       *node;
    mir_value           *value;
    cg_x86_64_map_value *cg;
  } a1;

  list_mir_arg_it it = list_mir_arg_begin(builtin->args);
  a1.node = it.get(&it);

  cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);

  a1.cg = ctx_sub_value_find(ctx, a1.value);

  ctx_stmt_builtin_f_value(ctx, symbol, 0, 1, a1.cg);
}

void ctx_stmt_builtin_greater(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
  if (list_mir_arg_size(builtin->args) != 2) {
    error("expected 2 args %p", builtin);
    return;
  }

  union {
    mir_node_base       *node;
    mir_value           *value;
    cg_x86_64_map_value *cg;
  } a1, a2;

  list_mir_arg_it it = list_mir_arg_begin(builtin->args);
  a1.node = it.get(&it);
  it.next(&it);
  a2.node = it.get(&it);

  cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);
  cg_mir_node_kind_assign(a2.node, a2.node->kind, a2.value, MIR_NODE_VALUE);

  a1.cg = ctx_sub_value_find(ctx, a1.value);
  a2.cg = ctx_sub_value_find(ctx, a2.value);

  ctx_stmt_builtin_f_value(ctx, X86_64_SYMBOL_LESS, 0, 2, a2.cg, a1.cg);
}

void ctx_stmt_builtin_greater_eq(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
  if (list_mir_arg_size(builtin->args) != 2) {
    error("expected 2 args %p", builtin);
    return;
  }

  union {
    mir_node_base       *node;
    mir_value           *value;
    cg_x86_64_map_value *cg;
  } a1, a2;

  list_mir_arg_it it = list_mir_arg_begin(builtin->args);
  a1.node = it.get(&it);
  it.next(&it);
  a2.node = it.get(&it);

  cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);
  cg_mir_node_kind_assign(a2.node, a2.node->kind, a2.value, MIR_NODE_VALUE);

  a1.cg = ctx_sub_value_find(ctx, a1.value);
  a2.cg = ctx_sub_value_find(ctx, a2.value);

  ctx_stmt_builtin_f_value(ctx, X86_64_SYMBOL_LESS_EQ, 0, 2, a2.cg, a1.cg);
}

static void ctx_stmt_builtin_call(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
  if (list_mir_arg_size(builtin->args) == 0) {
    error("expected at least 1 arg (nothing to be called) %p", builtin);
    return;
  }

  union {
    mir_node_base      *node;
    mir_subroutine     *sub;
    mir_subroutine_ext *ext;
  } fun;

  list_mir_arg_it it = list_mir_arg_begin(builtin->args);
  fun.node = it.get(&it);

  switch (fun.node->kind) {
  case MIR_NODE_SUBROUTINE: {
    list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();

    union {
      mir_node_base       *node;
      mir_value           *value;
      cg_x86_64_map_value *cg;
    } arg;

    while (it.next(&it)) {
      arg.node = it.get(&it);

      if (arg.node->kind != MIR_NODE_VALUE) {
        error("expected %d as argument, got %d %p", MIR_NODE_VALUE,
              arg.node->kind, arg.node);
        goto release;
      }
      arg.value = (mir_value *)arg.node;
      arg.cg = ctx_sub_value_find(ctx, arg.value);

      list_cg_x86_64_map_value_push_back(values, arg.cg);
    }

    size_t stack_size = ctx_args_value_prepare(ctx, 0, values, 0);

    // call func
    list_cg_x86_64_op *operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_direct(ctx_sub_name(ctx, fun.sub)));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

    ctx_stack_release(ctx, stack_size);

  release:
    while (!list_cg_x86_64_map_value_empty(values)) {
      list_cg_x86_64_map_value_pop_front(values);
    }
    list_cg_x86_64_map_value_free(values);
  } break;
    // unwrap arguments based on type
    // if argument pointer -> pass __x86_64_unwrap_array
    // if result void -> __x86_64_value_void()
    // if result pointer -> __x86_64_value_array with 1 element
  case MIR_NODE_SUBROUTINE_EXTERNAL: {
    list_cg_x86_64_op *operands;
    list_type_it       t_it = list_type_begin(fun.ext->args);
    size_t             argc = 0;
    size_t             prev_stack = ctx->sub_stack_size;

    list_cg_x86_64_unit *text_units = list_cg_x86_64_unit_new();

    // NOTE:
    // 1. unwrap arguments (pushed to front, so reverse order)
    // 2. stack here isn't aligned, but doesn't matter (too lazy)
    for (it.next(&it); !it.end(&it); it.next(&it), t_it.next(&t_it), ++argc) {
      mir_node_base *node = it.get(&it);
      type_base     *type = t_it.get(&t_it);

      if (node->kind != MIR_NODE_VALUE) {
        error("expected VALUE(%d) kind, got %d", MIR_NODE_VALUE, node->kind);
        break;
      }
      mir_value           *value = (mir_value *)node;
      cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);

      // pushq %rax (arguments are aligned before push)
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
      list_cg_x86_64_unit_push_front(text_units,
                                     (cg_x86_64_unit *)cg_x86_64_instr_new(
                                         CG_X86_64_MNEM_PUSHQ, operands));

      // call <__unwrap_*>
      operands = list_cg_x86_64_op_new();
      switch (type->type) {
      case TYPE_BOOL:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_BOOL));
        break;
      case TYPE_INT:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_INT));
        break;
      case TYPE_UINT:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_UINT));
        break;
      case TYPE_LONG:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_LONG));
        break;
      case TYPE_ULONG:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_ULONG));
        break;
      case TYPE_CHAR:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_CHAR));
        break;
      case TYPE_STRING:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_STRING));
        break;
      case TYPE_ARRAY:
        cg_op_push_back(operands,
                        cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_ARRAY));
        break;
      default:
        error("unsupported type %d %p", type->type, builtin);
        break;
      }
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

      // mov val, %rdi
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_base(cg_value->rbp_offset,
                                                      CG_X86_64_REG_RBP));
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
    }

    // calculate stack offset (at max 6 arguments will be popped)
    ctx->sub_stack_size += (argc > 6 ? argc - 6 : 0) * X86_64_ALIGN_MEMORY;

    size_t align = cg_with_align(ctx->sub_stack_size, X86_64_ALIGN_CALL) -
                   ctx->sub_stack_size;

    // align stack before pushes
    if (align) {
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_immediate(align));
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
      ctx_text_push_back(ctx,
                         cg_x86_64_instr_new(CG_X86_64_MNEM_SUBQ, operands));
      ctx->sub_stack_size += align;
    }

    ctx_text_push_back_many(ctx, text_units);

    // pop 6 arguments from stack (in reverse order)
    text_units = list_cg_x86_64_unit_new();
    switch (argc > 6 ? 6 : argc) {
    case 6:
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));
      __attribute__((fallthrough));
    case 5:
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));
      __attribute__((fallthrough));
    case 4:
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));
      __attribute__((fallthrough));
    case 3:
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));
      __attribute__((fallthrough));
    case 2:
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));
      __attribute__((fallthrough));
    case 1:
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
      list_cg_x86_64_unit_push_front(
          text_units,
          (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));
      __attribute__((fallthrough));
    default:
      break;
    }
    ctx_text_push_back_many(ctx, text_units);

    // call external function
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_direct(ctx_ext_name(ctx, fun.ext)));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

    // movq %rax, %rdi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    // call __cg_x86_64_value for something (only one argument)
    operands = list_cg_x86_64_op_new();
    switch (fun.ext->ret->type) {
    case TYPE_BOOL:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BOOL));
      break;
    case TYPE_BYTE:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BYTE));
      break;
    case TYPE_INT:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INT));
      break;
    case TYPE_UINT:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_UINT));
      break;
    case TYPE_LONG:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_LONG));
      break;
    case TYPE_ULONG:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ULONG));
      break;
    case TYPE_CHAR:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_CHAR));
      break;
    case TYPE_STRING:
      cg_op_push_back(
          operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_STRING_MOVE));
      break;
    case TYPE_VOID:
      cg_op_push_back(operands,
                      cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_VOID));
      break;
    default:
      error("unexpected ret type %d", fun.ext->ret->type);
      break;
    }
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

    // restore stack
    if (ctx->sub_stack_size - prev_stack) {
      operands = list_cg_x86_64_op_new();
      cg_op_push_back(operands, cg_x86_64_op_new_immediate(ctx->sub_stack_size -
                                                           prev_stack));
      cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
      ctx_text_push_back(ctx,
                         cg_x86_64_instr_new(CG_X86_64_MNEM_ADDQ, operands));
    }

  } break;
  default:
    error("expected subroutine, got %d %p", fun.node->kind, fun.node);
    return;
  }
}

void ctx_stmt_builtin_index(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
  list_cg_x86_64_op        *operands;
  list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();

  for (list_mir_arg_it it = list_mir_arg_begin(builtin->args); !it.end(&it);
       it.next(&it)) {
    mir_node_base *node = it.get(&it);
    if (node->kind != MIR_NODE_VALUE) {
      error("expected VALUE(%d) kind, got %d", MIR_NODE_VALUE, node->kind);
      goto release;
    }
    mir_value           *value = (mir_value *)node;
    cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);
    list_cg_x86_64_map_value_push_back(values, cg_value);
  }

  size_t stack_size = ctx_args_value_prepare(ctx, 1, values, 0);

  // call func
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_INDEX_GET));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  ctx_stack_release(ctx, stack_size);

release:
  while (!list_cg_x86_64_map_value_empty(values)) {
    list_cg_x86_64_map_value_pop_front(values);
  }
  list_cg_x86_64_map_value_free(values);
}

// TODO: complex types
void ctx_stmt_builtin_make(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                           mir_value *lsv, mir_builtin *builtin) {
  list_cg_x86_64_op *operands;

  type_enum spec_type = builtin->spec->type;

  switch (spec_type) {
  case TYPE_BOOL: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BOOL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_BYTE: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BYTE));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_INT: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INT));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_UINT: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_UINT));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_LONG: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_LONG));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_ULONG: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ULONG));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_CHAR: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_CHAR));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_STRING: {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_STRING));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
  } break;
  case TYPE_ARRAY: {
    type_base *elem_type = ((type_array *)builtin->spec)->elem_type;

    x86_64_type *type = cg_type_to_x86_64(elem_type);
    ctx_data_push_back(ctx, cg_x86_64_label_new(ctx_data_name(ctx, sub, lsv),
                                                CG_X86_64_LABEL_DATA));
    ctx_type_serialize(ctx, type);
    x86_64_type_free(type);

    // leaq .Ldata, %rdi
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_direct(ctx_data_name(ctx, sub, lsv)));
    cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ, operands));

    // mov variadic arguments
    list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();
    for (list_mir_arg_it it = list_mir_arg_begin(builtin->args); !it.end(&it);
         it.next(&it)) {
      mir_node_base *node = it.get(&it);
      if (node->kind != MIR_NODE_VALUE) {
        error("expected VALUE(%d) kind, got %d", MIR_NODE_VALUE, node->kind);
        goto release;
      }
      mir_value           *value = (mir_value *)node;
      cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);
      list_cg_x86_64_map_value_push_back(values, cg_value);
    }
    size_t stack_size = ctx_args_value_prepare(ctx, 1, values, 1);

    // call func
    list_cg_x86_64_op *operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ARRAY));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

    ctx_stack_release(ctx, stack_size);

  release:
    while (!list_cg_x86_64_map_value_empty(values)) {
      list_cg_x86_64_map_value_pop_front(values);
    }
    list_cg_x86_64_map_value_free(values);
  } break;
  default:
    error("unsupported new type %s %p", type_enum_str(spec_type), builtin);
    return;
  }
}

void ctx_stmt_builtin_cast(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
  if (list_mir_arg_size(builtin->args) != 1) {
    error("expected 1 arg %p", builtin);
    return;
  }

  list_cg_x86_64_op *operands;
  union {
    mir_node_base       *node;
    mir_value           *value;
    cg_x86_64_map_value *cg;
  } arg;

  {
    arg.node = list_mir_arg_front(builtin->args);
    if (arg.node->kind != MIR_NODE_VALUE) {
      error("expected %d as argument, got %d %p", MIR_NODE_VALUE,
            arg.node->kind, arg.node);
      return;
    }

    arg.value = (mir_value *)arg.node;
    arg.cg = ctx_sub_value_find(ctx, arg.value);
  }

  type_enum        spec_type = builtin->spec->type;
  x86_64_type_enum type;
  switch (spec_type) {
  case TYPE_BOOL:
    type = X86_64_TYPE_BOOL;
    break;
  case TYPE_BYTE:
    type = X86_64_TYPE_BYTE;
    break;
  case TYPE_INT:
    type = X86_64_TYPE_INT;
    break;
  case TYPE_UINT:
    type = X86_64_TYPE_UINT;
    break;
  case TYPE_LONG:
    type = X86_64_TYPE_LONG;
    break;
  case TYPE_ULONG:
    type = X86_64_TYPE_ULONG;
    break;
  case TYPE_CHAR:
    type = X86_64_TYPE_CHAR;
    break;
  case TYPE_STRING:
    type = X86_64_TYPE_STRING;
    break;
  case TYPE_VOID:
    type = X86_64_TYPE_VOID;
    break;
  case TYPE_ARRAY:
    type = X86_64_TYPE_ARRAY;
    break;
  default:
    error("unsupported cast type %s is not supported",
          type_enum_str(spec_type));
    type = X86_64_TYPE_VOID;
  }

  uint64_t type_i = type;

  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(arg.cg->rbp_offset, CG_X86_64_REG_RBP));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_immediate(type_i));
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_CAST));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
}

void ctx_stmt_store_builtin(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                            mir_value *lsv, mir_builtin *rsv) {
  cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, lsv);

  ctx_sub_value_drop(ctx, cg_lsv);

  switch (rsv->kind) {
  case MIR_BUILTIN_PLUS:
    ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_PLUS);
    break;
  case MIR_BUILTIN_MINUS:
    ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_MINUS);
    break;
  case MIR_BUILTIN_LOGICAL_NOT:
    ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_NOT);
    break;
  case MIR_BUILTIN_BITWISE_NOT:
    ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_BIT_NOT);
    break;
  case MIR_BUILTIN_INC:
    ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_INC);
    break;
  case MIR_BUILTIN_DEC:
    ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_DEC);
    break;
  case MIR_BUILTIN_LOGICAL_OR:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_OR);
    break;
  case MIR_BUILTIN_LOGICAL_AND:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_AND);
    break;
  case MIR_BUILTIN_BITWISE_OR:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_OR);
    break;
  case MIR_BUILTIN_BITWISE_XOR:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_XOR);
    break;
  case MIR_BUILTIN_BITWISE_AND:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_AND);
    break;
  case MIR_BUILTIN_EQUALS:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_EQ);
    break;
  case MIR_BUILTIN_NOT_EQUALS:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_NEQ);
    break;
  case MIR_BUILTIN_LESS:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_LESS);
    break;
  case MIR_BUILTIN_LESS_EQUALS:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_LESS_EQ);
    break;
  case MIR_BUILTIN_GREATER:
    ctx_stmt_builtin_greater(ctx, rsv);
    break;
  case MIR_BUILTIN_GREATER_EQUALS:
    ctx_stmt_builtin_greater_eq(ctx, rsv);
    break;
  case MIR_BUILTIN_BITWISE_SHIFT_LEFT:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_SHL);
    break;
  case MIR_BUILTIN_BITWISE_SHIFT_RIGHT:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_SHR);
    break;
  case MIR_BUILTIN_ADD:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_ADD);
    break;
  case MIR_BUILTIN_SUB:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_SUB);
    break;
  case MIR_BUILTIN_MUL:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_MUL);
    break;
  case MIR_BUILTIN_DIV:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_DIV);
    break;
  case MIR_BUILTIN_MOD:
    ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_MOD);
    break;
  case MIR_BUILTIN_CALL:
    ctx_stmt_builtin_call(ctx, rsv);
    break;
  case MIR_BUILTIN_INDEX:
    ctx_stmt_builtin_index(ctx, rsv);
    break;
  case MIR_BUILTIN_CAST:
    ctx_stmt_builtin_cast(ctx, rsv);
    break;
  case MIR_BUILTIN_MAKE:
    ctx_stmt_builtin_make(ctx, sub, lsv, rsv);
    break;
  case MIR_BUILTIN_PRINT:
    ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_PRINT);
    break;
  default:
    error("unexpected store builtin kind %d %p", rsv->kind, rsv);
    return;
  }

  list_cg_x86_64_op *operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
  cg_op_push_back(operands,
                  cg_x86_64_op_new_base(cg_lsv->rbp_offset, CG_X86_64_REG_RBP));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
}

void ctx_stmt_store_to_builtin(cg_x86_64_ctx *ctx, mir_builtin *builtin,
                               mir_value *rsv) {
  list_cg_x86_64_op        *operands;
  list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();

  // from
  cg_x86_64_map_value *cg_rsv = ctx_sub_value_find(ctx, rsv);
  list_cg_x86_64_map_value_push_back(values, cg_rsv);

  // to
  for (list_mir_arg_it it = list_mir_arg_begin(builtin->args); !it.end(&it);
       it.next(&it)) {
    mir_node_base *node = it.get(&it);
    if (node->kind != MIR_NODE_VALUE) {
      error("expected VALUE(%d) kind, got %d", MIR_NODE_VALUE, node->kind);
      goto release;
    }
    mir_value           *value = (mir_value *)node;
    cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);
    list_cg_x86_64_map_value_push_back(values, cg_value);
  }

  size_t stack_size = ctx_args_value_prepare(ctx, 1, values, 0);

  // call func
  operands = list_cg_x86_64_op_new();
  cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_INDEX_SET));
  ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));

  ctx_stack_release(ctx, stack_size);

release:
  while (!list_cg_x86_64_map_value_empty(values)) {
    list_cg_x86_64_map_value_pop_front(values);
  }
  list_cg_x86_64_map_value_free(values);
}

static void ctx_stmt_store(cg_x86_64_ctx *ctx, mir_subroutine *sub,
                           mir_builtin *stmt) {
  if (list_mir_arg_size(stmt->args) != 2) {
    error("store requires 2 args");
    goto release;
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
      ctx_stmt_store_builtin(ctx, sub, (mir_value *)first,
                             (mir_builtin *)second);
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
release:
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

  cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, lsv.value);
  ctx_sub_value_drop(ctx, cg_lsv);
}

// no jump only on last block
static void ctx_jump(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_jmp *jmp) {
  list_cg_x86_64_op *operands;

  if (!jmp) {
    operands = list_cg_x86_64_op_new();
    cg_op_push_back(operands,
                    cg_x86_64_op_new_direct(ctx_sub_epilogue_name(ctx, sub)));
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_JMP, operands));
    return;
  }

  // conditional
  if (jmp->value_ref) {
    cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, jmp->value_ref);

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
    ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_JNZ, operands));
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

static void ctx_sub(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
  ctx->sub_stack_size = X86_64_ALIGN_REG_64;
  ctx->sub_map_values = hashset_cg_x86_64_map_value_new();

  ctx_text_push_back(
      ctx, cg_x86_64_label_new(ctx_sub_name(ctx, sub), CG_X86_64_LABEL_INSTR));

  ctx_sub_prologue(ctx);
  ctx_sub_values_prologue(ctx, sub);
  ctx_sub_args_prologue(ctx, sub);

  for (list_mir_bb_it it = list_mir_bb_begin(sub->bbs); !it.end(&it);
       it.next(&it)) {
    ctx_block(ctx, sub, it.get(&it));
  }

  int exit = ctx_is_exit(ctx, sub);

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
    ctx_sub(ctx, it.get(&it));
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
