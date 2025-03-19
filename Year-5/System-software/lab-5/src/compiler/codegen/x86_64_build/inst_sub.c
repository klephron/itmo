#include "inst_sub.h"
#include "compiler/codegen/exception.h"
#include "compiler/codegen/x86_64/x86_64.h"
#include "util/log.h"
#include "util/macro.h"
#include "x86_64_core/value.h"
#include <string.h>

static void cg_inst_sub_def_prologue(cg_ctx *ctx, const mir_subroutine *sub) {
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_RBP),
      NULL);

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_MOVQ, cg_x86_64_op_new_register(CG_X86_64_REG_RSP),
      cg_x86_64_op_new_register(CG_X86_64_REG_RBP), NULL);

  // return value - passed as first argument, copy reference to stack
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_RDI),
      NULL);
  cg_ctx_value_meta_emplace(ctx, sub->defined.ret, cg_ctx_rbp_offset(ctx), 1);

  // copy references to params to stack (by AMD64 convention)
  list_mir_value_it it = list_mir_value_begin(sub->defined.params);

  if (END(it)) {
    return;
  }
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_RSI),
      NULL);
  cg_ctx_value_meta_emplace(ctx, GET(it), cg_ctx_rbp_offset(ctx), 1);

  if (!NEXT(it)) {
    return;
  }
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_RDX),
      NULL);
  cg_ctx_value_meta_emplace(ctx, GET(it), cg_ctx_rbp_offset(ctx), 1);

  if (!NEXT(it)) {
    return;
  }
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_RCX),
      NULL);
  cg_ctx_value_meta_emplace(ctx, GET(it), cg_ctx_rbp_offset(ctx), 1);

  if (!NEXT(it)) {
    return;
  }
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_R8),
      NULL);
  cg_ctx_value_meta_emplace(ctx, GET(it), cg_ctx_rbp_offset(ctx), 1);

  if (!NEXT(it)) {
    return;
  }
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_R9),
      NULL);
  cg_ctx_value_meta_emplace(ctx, GET(it), cg_ctx_rbp_offset(ctx), 1);

  // first pushed last (top), last pushed first
  int64_t param_rbp_offset = CG_X86_64_SIZE_QUAD * 2;
  while (NEXT(it)) {
    ctx->frame_size += cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_PUSHQ,
        cg_x86_64_op_new_base(param_rbp_offset, CG_X86_64_REG_RBP), NULL);
    cg_ctx_value_meta_emplace(ctx, GET(it), cg_ctx_rbp_offset(ctx), 1);

    param_rbp_offset += CG_X86_64_SIZE_QUAD;
  }
}

static void cg_inst_sub_def_locals_place(cg_ctx               *ctx,
                                         const mir_subroutine *sub) {
  for (list_mir_value_it it = list_mir_value_begin(sub->defined.vars); !END(it);
       NEXT(it)) {
    const mir_value *value = GET(it);
    ctx->frame_size += sizeof(x86_64_value);
    cg_ctx_value_meta_emplace(ctx, value, cg_ctx_rbp_offset(ctx), 0);
  }
  for (list_mir_value_it it = list_mir_value_begin(sub->defined.tmps); !END(it);
       NEXT(it)) {
    const mir_value *value = GET(it);
    ctx->frame_size += sizeof(x86_64_value);
    cg_ctx_value_meta_emplace(ctx, value, cg_ctx_rbp_offset(ctx), 0);
  }
}

static void cg_inst_sub_def_epilogue(cg_ctx *ctx, const mir_subroutine *sub) {
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_text(cg_sub_global_label_suf(sub, "epilogue")));

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_MOVQ, cg_x86_64_op_new_register(CG_X86_64_REG_RBP),
      cg_x86_64_op_new_register(CG_X86_64_REG_RSP), NULL);

  cg_ctx_text_emplace_back_text(ctx, CG_X86_64_MNEM_POPQ,
                                cg_x86_64_op_new_register(CG_X86_64_REG_RBP),
                                NULL);

  cg_ctx_text_emplace_back_text(ctx, CG_X86_64_MNEM_RETQ, NULL);
}

static void cg_inst_sub_def_locals_init(cg_ctx               *ctx,
                                        const mir_subroutine *sub) {
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_text(cg_sub_global_label_suf(sub, "init")));

  for (list_mir_value_it it = list_mir_value_begin(sub->defined.vars); !END(it);
       NEXT(it)) {
    const mir_value     *value = GET(it);
    const cg_value_meta *meta  = cg_ctx_value_meta_find(ctx, value);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_LEAQ,
        cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_CALL,
        cg_x86_64_op_new_direct(strdup("__x86_64_make_void")), NULL);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->defined.tmps); !END(it);
       NEXT(it)) {
    const mir_value     *value = GET(it);
    const cg_value_meta *meta  = cg_ctx_value_meta_find(ctx, value);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_LEAQ,
        cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_CALL,
        cg_x86_64_op_new_direct(strdup("__x86_64_make_void")), NULL);
  }
}

static void cg_inst_sub_def_locals_deinit(cg_ctx               *ctx,
                                          const mir_subroutine *sub) {
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_text(cg_sub_global_label_suf(sub, "deinit")));

  for (list_mir_value_it it = list_mir_value_begin(sub->defined.vars); !END(it);
       NEXT(it)) {
    const mir_value     *value = GET(it);
    const cg_value_meta *meta  = cg_ctx_value_meta_find(ctx, value);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_MOVQ,
        cg_x86_64_op_new_base(meta->offset + offsetof(x86_64_value, op_tbl),
                              CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_MOVQ,
        cg_x86_64_op_new_base(offsetof(x86_64_op_tbl, op_drop),
                              CG_X86_64_REG_RAX),
        cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_LEAQ,
        cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

    cg_ctx_text_emplace_back_text(ctx, CG_X86_64_MNEM_CALL,
                                  cg_x86_64_op_new_register(CG_X86_64_REG_RAX),
                                  NULL);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->defined.tmps); !END(it);
       NEXT(it)) {
    const mir_value     *value = GET(it);
    const cg_value_meta *meta  = cg_ctx_value_meta_find(ctx, value);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_MOVQ,
        cg_x86_64_op_new_base(meta->offset + offsetof(x86_64_value, op_tbl),
                              CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_MOVQ,
        cg_x86_64_op_new_base(offsetof(x86_64_op_tbl, op_drop),
                              CG_X86_64_REG_RAX),
        cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_LEAQ,
        cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

    cg_ctx_text_emplace_back_text(ctx, CG_X86_64_MNEM_CALL,
                                  cg_x86_64_op_new_register(CG_X86_64_REG_RAX),
                                  NULL);
  }
}

static int cg_inst_sub_def_ok(cg_ctx *ctx, const mir_subroutine *sub) {
  const char *sub_name = sub->symbol_ref->name;

  if (sub->kind != MIR_SUBROUTINE_DEFINED) {
    error("subroutine '%s' def got kind %d %p, expected %d", sub_name,
          sub->kind, sub, MIR_SUBROUTINE_DEFINED);
    return 0;
  }

  if (sub->spec & MIR_SUBROUTINE_SPEC_EXTERN) {
    cg_exception_add_error(
        ctx->exceptions, EXCEPTION_CG_UNEXPECTED_EXTERN_DEFINITION,
        sub->symbol_ref->span,
        "subroutine '%s' can't be instantiated: is defined, but extern",
        sub_name);
    return 0;
  }

  return 1;
}

static void cg_inst_sub_def(cg_ctx *ctx, const mir_subroutine *sub) {
  if (!cg_inst_sub_def_ok(ctx, sub)) {
    return;
  }

  cg_ctx_text_push_back(ctx,
                        cg_x86_64_symbol_new_text(cg_sub_global_label(sub)));

  ctx->map_value_meta = hashset_cg_value_meta_new();

  ctx->frame_size = CG_X86_64_SIZE_QUAD;

  cg_inst_sub_def_prologue(ctx, sub);

  uint64_t frame_size_old = ctx->frame_size;

  cg_inst_sub_def_locals_place(ctx, sub);

  // alloc memory for locals
  ctx->frame_size = cg_aligned(ctx->frame_size, 16);

  if (frame_size_old != ctx->frame_size) {
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_SUBQ,
        cg_x86_64_op_new_immediate(ctx->frame_size - frame_size_old),
        cg_x86_64_op_new_register(CG_X86_64_REG_RSP), NULL);
  }

  cg_inst_sub_def_locals_init(ctx, sub);

  // TODO: handle basic blocks

  cg_inst_sub_def_locals_deinit(ctx, sub);

  cg_inst_sub_def_epilogue(ctx, sub);

  hashset_cg_value_meta_free(ctx->map_value_meta);
}

static void cg_inst_sub_main_prologue(cg_ctx *ctx, const mir_subroutine *sub) {
  UNUSED(sub);
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_RBP),
      NULL);

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_MOVQ, cg_x86_64_op_new_register(CG_X86_64_REG_RSP),
      cg_x86_64_op_new_register(CG_X86_64_REG_RBP), NULL);
}

static void cg_inst_sub_main_epilogue(cg_ctx *ctx, const mir_subroutine *sub) {
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_text(cg_sub_global_label_suf(sub, "epilogue")));

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_CALL,
      cg_x86_64_op_new_direct(strdup("__x86_64_flush")), NULL);

  const type_base *ret_type =
      sub->defined.ret->type_ref ? sub->defined.ret->type_ref->type : NULL;

  if (ret_type && ret_type->kind == TYPE_PRIMITIVE &&
      ((type_primitive *)ret_type)->type == TYPE_PRIMITIVE_INT) {

    cg_value_meta *ret_meta = cg_ctx_value_meta_find(ctx, sub->defined.ret);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_LEAQ,
        cg_x86_64_op_new_base(ret_meta->offset, CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_CALL,
        cg_x86_64_op_new_direct(strdup("__x86_64_unwrap_int")), NULL);

    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_MOVL, cg_x86_64_op_new_register(CG_X86_64_REG_RAX),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);
  } else {
    cg_exception_add_error(ctx->exceptions, EXCEPTION_CG_UNEXPECTED_RETURN_TYPE,
                           sub->symbol_ref->span,
                           "expected subroutine '%s' return type int",
                           sub->symbol_ref->name);
  }

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_MOVQ, cg_x86_64_op_new_immediate(60),
      cg_x86_64_op_new_register(CG_X86_64_REG_RAX), NULL);

  cg_ctx_text_emplace_back_text(ctx, CG_X86_64_MNEM_SYSCALL, NULL);
}

static void cg_inst_sub_main(cg_ctx *ctx, const mir_subroutine *sub) {
  if (!cg_inst_sub_def_ok(ctx, sub)) {
    return;
  }

  cg_ctx_text_push_back(ctx,
                        cg_x86_64_symbol_new_text(cg_sub_global_label(sub)));

  ctx->map_value_meta = hashset_cg_value_meta_new();

  ctx->frame_size = CG_X86_64_SIZE_QUAD;

  cg_inst_sub_main_prologue(ctx, sub);

  uint64_t frame_size_old = ctx->frame_size;

  // place return value on stack
  ctx->frame_size += sizeof(x86_64_value);
  cg_value_meta *ret_meta = cg_ctx_value_meta_emplace(
      ctx, sub->defined.ret, cg_ctx_rbp_offset(ctx), 0);

  cg_inst_sub_def_locals_place(ctx, sub);

  // alloc memory for locals
  ctx->frame_size = cg_aligned(ctx->frame_size, 16);

  if (frame_size_old != ctx->frame_size) {
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_SUBQ,
        cg_x86_64_op_new_immediate(ctx->frame_size - frame_size_old),
        cg_x86_64_op_new_register(CG_X86_64_REG_RSP), NULL);
  }

  // init return value
  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_LEAQ,
      cg_x86_64_op_new_base(ret_meta->offset, CG_X86_64_REG_RBP),
      cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_CALL,
      cg_x86_64_op_new_direct(strdup("__x86_64_make_void")), NULL);

  cg_inst_sub_def_locals_init(ctx, sub);

  // TODO: handle basic blocks

  cg_inst_sub_def_locals_deinit(ctx, sub);

  cg_inst_sub_main_epilogue(ctx, sub);

  hashset_cg_value_meta_free(ctx->map_value_meta);
}

cg_inst_sub_result cg_inst_subs(cg_x86_64 *code, const mir *mir) {
  cg_inst_sub_result result = {
      .exceptions = list_exception_new(),
  };

  cg_ctx ctx;
  cg_ctx_init(&ctx, code, result.exceptions);

  for (list_mir_subroutine_it it =
           list_mir_subroutine_begin(mir->imported_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *sub = GET(it);
    cg_exception_add_error(ctx.exceptions, EXCEPTION_CG_UNSUPPORTED_IMPORTED,
                           sub->symbol_ref->span,
                           "subroutine '%s' can't be instantiated",
                           sub->symbol_ref->name);
  }

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->defined_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *sub = GET(it);

    int is_main = !strcmp(sub->symbol_ref->name, "main");
    if (is_main) {
      cg_inst_sub_main(&ctx, sub);
    } else {
      cg_inst_sub_def(&ctx, sub);
    }
  }

  cg_ctx_deinit(&ctx);

  return result;
}
