#include "inst_sub.h"

#include "util/macro.h"

cg_value_meta *cg_value_meta_new(const mir_value *value_ref, int64_t offset,
                                 int is_ptr) {
  cg_value_meta *self = MALLOC(cg_value_meta);
  self->value_ref     = value_ref;
  self->offset        = offset;
  self->is_ptr        = is_ptr;
  return self;
}

void cg_value_meta_free(cg_value_meta *self) {
  if (self) {
    free(self);
  }
}

void cg_ctx_init(cg_ctx *ctx, cg_x86_64 *code, list_exception *exceptions) {
  ctx->code       = code;
  ctx->exceptions = exceptions;

  ctx->sub            = NULL;
  ctx->map_value_meta = NULL;
  ctx->frame_size     = 0;

  ctx->bb = NULL;
}

void cg_ctx_deinit(cg_ctx *ctx) {
  ctx->code       = NULL;
  ctx->exceptions = NULL;

  ctx->sub            = NULL;
  ctx->map_value_meta = NULL;
  ctx->frame_size     = 0;

  ctx->bb = NULL;
}

cg_value_meta *cg_ctx_value_meta_find(cg_ctx *ctx, const mir_value *value) {
  hashset_cg_value_meta_it it = hashset_cg_value_meta_find(
      ctx->map_value_meta, &(cg_value_meta){.value_ref = value});
  if (END(it)) {
    return NULL;
  }
  return GET(it);
}

cg_value_meta *cg_ctx_value_meta_emplace(cg_ctx          *ctx,
                                         const mir_value *value_ref,
                                         int64_t offset, int is_ptr) {
  hashset_cg_value_meta_it it = hashset_cg_value_meta_insert(
      ctx->map_value_meta, cg_value_meta_new(value_ref, offset, is_ptr));
  return GET(it);
}

void cg_ctx_text_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->text, (cg_x86_64_unit *)unit);
}

void cg_ctx_data_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->data, (cg_x86_64_unit *)unit);
}

void cg_ctx_debug_info_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->debug_info, (cg_x86_64_unit *)unit);
}

void cg_ctx_debug_line_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->debug_line, (cg_x86_64_unit *)unit);
}

void cg_ctx_debug_str_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->debug_str, (cg_x86_64_unit *)unit);
}

// args of type cg_x86_64_op*, last arg is NULL
uint64_t cg_ctx_text_emplace_back_text(cg_ctx *ctx, cg_x86_64_mnem mnem, ...) {
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

int64_t cg_ctx_rbp_offset(cg_ctx *ctx) {
  return (-ctx->frame_size) + CG_X86_64_SIZE_QUAD * 2;
}
