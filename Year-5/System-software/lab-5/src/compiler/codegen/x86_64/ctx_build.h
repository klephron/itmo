// #pragma once

// #include "compiler/codegen/x86_64/ctx.h"

// #include "x86_64_core/meta.h"
// #include "x86_64_core/type.h"

// // NOTE: no name collisions (not public to user)

// // utils
// void ctx_add_error(cg_x86_64_ctx *ctx, exception_subtype_cg subtype,
//                    const char *format, ...);

// void   cg_op_push_back(list_cg_x86_64_op *operands, cg_x86_64_op *operand);
// size_t cg_with_align(size_t size, size_t align);
// void   cg_units_extend_move(list_cg_x86_64_unit *to, list_cg_x86_64_unit
// *from); cg_x86_64_op *cg_mode_new_direct_symbol(x86_64_symbol_enum kind);
// x86_64_type  *cg_type_to_x86_64(hir_type_base *type);
// void          cg_units_push_back(list_cg_x86_64_unit *to, void *from);
// void          cg_units_push_front(list_cg_x86_64_unit *to, void *from);

// int ctx_in_globals(cg_x86_64_ctx *ctx, const mir_node_base *base);
// int ctx_is_exit(cg_x86_64_ctx *ctx, const mir_subroutine *sub);

// static inline void ctx_data_push_back(cg_x86_64_ctx *ctx, void *data) {
//   list_cg_x86_64_unit_push_back(ctx->cg_ref->data->units,
//                                 (cg_x86_64_unit *)data);
// }
// static inline void ctx_text_push_back(cg_x86_64_ctx *ctx, void *text) {
//   list_cg_x86_64_unit_push_back(ctx->cg_ref->text->units,
//                                 (cg_x86_64_unit *)text);
// }
// static inline void ctx_text_push_back_many(cg_x86_64_ctx       *ctx,
//                                            list_cg_x86_64_unit *other) {
//   cg_units_extend_move(ctx->cg_ref->text->units, other);
// }

// cg_x86_64_map_value *ctx_sub_value_find(cg_x86_64_ctx *ctx, mir_value
// *value);

// void ctx_type_serialize(cg_x86_64_ctx *ctx, x86_64_type *type);
// void ctx_stack_release(cg_x86_64_ctx *ctx, size_t stack_size);

// // labels
// char *ctx_local_label(cg_x86_64_ctx *ctx, const char *old);
// char *ctx_block_label(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_bb *bb);
// char *ctx_data_label(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_value
// *value); char *ctx_sub_label(cg_x86_64_ctx *ctx, mir_subroutine *sub); char
// *ctx_ext_label(cg_x86_64_ctx *ctx, mir_subroutine_ext *ext); char
// *ctx_sub_epilogue_label(cg_x86_64_ctx *ctx, mir_subroutine *sub);

// // debug
// char *ctx_debug_sub_start(cg_x86_64_ctx *ctx, uint64_t sub_id);
// char *ctx_debug_sub_end(cg_x86_64_ctx *ctx, uint64_t sub_id);
// char *ctx_debug_sub_prologue_end(cg_x86_64_ctx *ctx, uint64_t sub_id);
// char *ctx_debug_sub_epilogue_start(cg_x86_64_ctx *ctx, uint64_t sub_id);
// char *ctx_debug_line(cg_x86_64_ctx *ctx, uint64_t id);
// void  ctx_debug_add_sub(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                         uint64_t sub_id);
// void  ctx_debug_add_line(cg_x86_64_ctx *ctx, mir_debug_line *line,
//                          uint64_t line_id);
// void  ctx_debug_info_post_push_header(cg_x86_64_ctx *ctx);
// void  ctx_debug_info_post_push_files(cg_x86_64_ctx *ctx);
// void  ctx_debug_line_post_push_header(cg_x86_64_ctx *ctx);
// void  ctx_debug_str_post_push_header(cg_x86_64_ctx *ctx);

// // sub
// void ctx_build_sub(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                    size_t debug_sub_id);

// // body
// void ctx_build_block(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_bb *bb);
