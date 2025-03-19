#include "inst_sub.h"
#include "compiler/codegen/exception.h"
#include "compiler/codegen/x86_64/x86_64.h"
#include "util/hashset.h"
#include "util/log.h"
#include "util/macro.h"
#include "x86_64_core/value.h"
#include <string.h>

// VALUE_OFFSET
typedef struct cg_value_meta_struct {
  const mir_value *value_ref;
  int64_t          offset; // of stack relative to rbp
  int              is_ptr; // is pointer of actual value
} cg_value_meta;

static cg_value_meta *cg_value_meta_new(const mir_value *value_ref,
                                        int64_t offset, int is_ptr) {
  cg_value_meta *self = MALLOC(cg_value_meta);
  self->value_ref     = value_ref;
  self->offset        = offset;
  self->is_ptr        = is_ptr;
  return self;
}

static void cg_value_meta_free(cg_value_meta *self) {
  if (self) {
    free(self);
  }
}

static int container_cmp_cg_value_meta(const void *lsv, const void *rsv) {
  const cg_value_meta *l = lsv;
  const cg_value_meta *r = rsv;
  return container_cmp_ptr(l->value_ref, r->value_ref);
}
static uint64_t container_hash_cg_value_meta(const void *lsv) {
  const cg_value_meta *l = lsv;
  return container_hash_ptr(l->value_ref);
}
static void container_delete_cg_value_meta(void *data) {
  return cg_value_meta_free(data);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_cg_value_meta, cg_value_meta,
                              container_cmp_cg_value_meta, container_new_move,
                              container_delete_cg_value_meta,
                              container_hash_cg_value_meta);

// CTX
typedef struct cg_ctx_struct {
  cg_x86_64      *code;
  list_exception *exceptions;

  hashset_cg_value_meta *map_value_meta;
  uint64_t               frame_size;
} cg_ctx;

static void cg_ctx_init(cg_ctx *ctx, cg_x86_64 *code,
                        list_exception *exceptions) {
  ctx->code       = code;
  ctx->exceptions = exceptions;

  ctx->map_value_meta = NULL;
  ctx->frame_size     = 0;
}

static void cg_ctx_deinit(cg_ctx *ctx) {
  ctx->code       = NULL;
  ctx->exceptions = NULL;

  ctx->map_value_meta = NULL;
  ctx->frame_size     = 0;
}

static cg_value_meta *cg_ctx_value_meta_find(cg_ctx          *ctx,
                                             const mir_value *value) {
  hashset_cg_value_meta_it it = hashset_cg_value_meta_find(
      ctx->map_value_meta, &(cg_value_meta){.value_ref = value});
  if (END(it)) {
    return NULL;
  }
  return GET(it);
}

static cg_value_meta *cg_ctx_value_meta_emplace(cg_ctx          *ctx,
                                                const mir_value *value_ref,
                                                int64_t offset, int is_ptr) {
  hashset_cg_value_meta_it it = hashset_cg_value_meta_insert(
      ctx->map_value_meta, cg_value_meta_new(value_ref, offset, is_ptr));
  return GET(it);
}

static inline void cg_ctx_text_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->text, (cg_x86_64_unit *)unit);
}

static inline void cg_ctx_data_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->data, (cg_x86_64_unit *)unit);
}

static inline void cg_ctx_debug_info_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->debug_info, (cg_x86_64_unit *)unit);
}

static inline void cg_ctx_debug_line_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->debug_line, (cg_x86_64_unit *)unit);
}

static inline void cg_ctx_debug_str_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->debug_str, (cg_x86_64_unit *)unit);
}

// args of type cg_x86_64_op*, last arg is NULL
static inline uint64_t cg_ctx_text_emplace_back_text(cg_ctx        *ctx,
                                                     cg_x86_64_mnem mnem, ...) {
  va_list args;
  va_start(args, mnem);

  list_cg_x86_64_op *operands = list_cg_x86_64_op_new();

  cg_x86_64_op *op = NULL;
  while ((op = va_arg(args, cg_x86_64_op *))) {
    list_cg_x86_64_op_push_back(operands, op);
  }

  cg_x86_64_text *text = cg_x86_64_text_new(mnem, operands);
  cg_ctx_text_push_back(ctx, text);

  va_end(args);

  return cg_x86_64_mnem_size(mnem);
}

static inline int64_t cg_rbp_offset(uint64_t frame_size) {
  return (-frame_size) + CG_X86_64_SIZE_QUAD * 2;
}

static inline int64_t cg_ctx_rbp_offset(cg_ctx *ctx) {
  return cg_rbp_offset(ctx->frame_size);
}

// returns additional memory allocated
static void cg_inst_sub_def_prologue(cg_ctx *ctx, const mir_subroutine *sub) {
  // pushq %rbp
  ctx->frame_size += cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_PUSHQ, cg_x86_64_op_new_register(CG_X86_64_REG_RBP),
      NULL);
  // movq %rsp, %rbp
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

static void cg_inst_sub_def_locals(cg_ctx *ctx, const mir_subroutine *sub) {
  uint64_t old_frame_size = ctx->frame_size;

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

  if (old_frame_size != ctx->frame_size) {
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_SUBQ,
        cg_x86_64_op_new_immediate(ctx->frame_size - old_frame_size),
        cg_x86_64_op_new_register(CG_X86_64_REG_RSP), NULL);
  }
}

static void cg_inst_sub_def_epilogue(cg_ctx *ctx, const mir_subroutine *sub) {
  // movq %rbp, %rsp
  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_MOVQ, cg_x86_64_op_new_register(CG_X86_64_REG_RBP),
      cg_x86_64_op_new_register(CG_X86_64_REG_RSP), NULL);
  // popq %rbp
  cg_ctx_text_emplace_back_text(ctx, CG_X86_64_MNEM_POPQ,
                                cg_x86_64_op_new_register(CG_X86_64_REG_RBP),
                                NULL);
  ctx->frame_size = 0;
}

static void cg_inst_sub_def_initializers(cg_ctx               *ctx,
                                         const mir_subroutine *sub) {
  for (list_mir_value_it it = list_mir_value_begin(sub->defined.vars); !END(it);
       NEXT(it)) {
    const mir_value     *value = GET(it);
    const cg_value_meta *meta  = cg_ctx_value_meta_find(ctx, value);

    // leaq <var_addr> %rdi
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_LEAQ,
        cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

    // call __x86_64_make_void
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_CALL,
        cg_x86_64_op_new_direct(strdup("__x86_64_make_void")), NULL);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->defined.tmps); !END(it);
       NEXT(it)) {
    const mir_value     *value = GET(it);
    const cg_value_meta *meta  = cg_ctx_value_meta_find(ctx, value);

    // leaq <var_addr> %rdi
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_LEAQ,
        cg_x86_64_op_new_base(meta->offset, CG_X86_64_REG_RBP),
        cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

    // call __x86_64_make_void
    cg_ctx_text_emplace_back_text(
        ctx, CG_X86_64_MNEM_CALL,
        cg_x86_64_op_new_direct(strdup("__x86_64_make_void")), NULL);
  }

  const cg_value_meta *meta_ret = cg_ctx_value_meta_find(ctx, sub->defined.ret);
  // leaq <var_addr> %rdi
  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_LEAQ,
      cg_x86_64_op_new_base(meta_ret->offset, CG_X86_64_REG_RBP),
      cg_x86_64_op_new_register(CG_X86_64_REG_RDI), NULL);

  // call __x86_64_make_void
  cg_ctx_text_emplace_back_text(
      ctx, CG_X86_64_MNEM_CALL,
      cg_x86_64_op_new_direct(strdup("__x86_64_make_void")), NULL);
}

static void cg_inst_sub_def(cg_ctx *ctx, const mir_subroutine *sub,
                            int is_exit) {
  const char *sub_name = sub->symbol_ref->name;

  if (sub->kind != MIR_SUBROUTINE_DEFINED) {
    error("subroutine '%s' def got kind %d %p, expected %d", sub_name,
          sub->kind, sub, MIR_SUBROUTINE_DEFINED);
    return;
  }

  if (sub->spec & MIR_SUBROUTINE_SPEC_EXTERN) {
    cg_exception_add_error(
        ctx->exceptions, EXCEPTION_CG_UNEXPECTED_EXTERN_DEFINITION,
        sub->symbol_ref->span,
        "subroutine '%s' can't be instantiated: is defined, but extern",
        sub_name);
    return;
  }

  cg_ctx_text_push_back(ctx, cg_x86_64_symbol_new_text(strdup(sub_name)));

  ctx->map_value_meta = hashset_cg_value_meta_new();

  ctx->frame_size = CG_X86_64_SIZE_QUAD;

  cg_inst_sub_def_prologue(ctx, sub);
  cg_inst_sub_def_locals(ctx, sub);

  cg_inst_sub_def_initializers(ctx, sub);

  // for (hashset_cg_value_meta_it it =
  //          hashset_cg_value_meta_begin(ctx->map_value_meta);
  //      !END(it); NEXT(it)) {
  //   cg_value_meta *value_meta = GET(it);
  //   debug("_%lu -> %ld(rbp),is_ptr=%d", value_meta->value_ref->id,
  //         value_meta->offset, value_meta->is_ptr);
  // }
  // debug("frame_size=%lu, rbp_offset=%ld", ctx->frame_size,
  //       cg_ctx_rbp_offset(ctx));

  cg_inst_sub_def_epilogue(ctx, sub);

  hashset_cg_value_meta_free(ctx->map_value_meta);
}

cg_inst_sub_result cg_inst_sub(cg_x86_64 *code, const mir *mir) {
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

    int is_exit = !strcmp(sub->symbol_ref->name, "main");
    cg_inst_sub_def(&ctx, sub, is_exit);
  }

  cg_ctx_deinit(&ctx);

  return result;
}
