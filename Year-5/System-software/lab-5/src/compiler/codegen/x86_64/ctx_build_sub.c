// #include "ctx_build.h"

// #include "x86_64_core/value.h"

// static void ctx_sub_prologue(cg_x86_64_ctx *ctx) {
//   list_cg_x86_64_op *operands;

//   // pushq %rbp
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_PUSHQ,
//   operands));
//   // movq %rsp, %rbp
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));

//   ctx->sub_stack_size += X86_64_ALIGN_REG_64;
//   ctx->sub_rbp_offset = ctx->sub_stack_size;
// }

// static void ctx_sub_epilogue(cg_x86_64_ctx *ctx, mir_subroutine *sub, int
// ret) {
//   list_cg_x86_64_op *operands;

//   ctx_text_push_back(ctx, cg_x86_64_label_new(ctx_sub_epilogue_label(ctx,
//   sub),
//                                               CG_X86_64_LABEL_INSTR));

//   // set rax to return value
//   if (sub->ret) {
//     operands = list_cg_x86_64_op_new();
//     cg_x86_64_map_value *cg_ret = ctx_sub_value_find(ctx, sub->ret);
//     cg_op_push_back(
//         operands, cg_x86_64_op_new_base(cg_ret->rbp_offset,
//         CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));
//   }

//   // movq %rsp, %rbp
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));
//   // popq %rbp
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ,
//   operands));

//   ctx->sub_stack_size -= X86_64_ALIGN_REG_64;
//   ctx->sub_rbp_offset = 0;

//   // ret
//   if (ret) {
//     operands = list_cg_x86_64_op_new();
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_RETQ,
//     operands)); ctx->sub_stack_size -= X86_64_ALIGN_REG_64;
//   }
// }

// static void ctx_sub_exit(cg_x86_64_ctx *ctx) {
//   list_cg_x86_64_op *operands;

//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_FFLUSH));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//   operands));

//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_immediate(60));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));

//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_XORQ,
//   operands));

//   operands = list_cg_x86_64_op_new();
//   ctx_text_push_back(ctx,
//                      cg_x86_64_instr_new(CG_X86_64_MNEM_SYSCALL, operands));
// }

// static void ctx_sub_values_prologue(cg_x86_64_ctx *ctx, mir_subroutine *sub)
// {
//   list_cg_x86_64_op *operands;

//   { // reserve memory on stack
//     size_t values = 0;

//     values += sub->ret ? 1 : 0;
//     values += list_mir_value_size(sub->args);
//     values += list_mir_value_size(sub->vars);
//     values += list_mir_value_size(sub->tmps);

//     size_t rsp_sub =
//         cg_with_align(values * sizeof(x86_64_value), X86_64_ALIGN_CALL);

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(rsp_sub));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_SUBQ,
//     operands));

//     ctx->sub_stack_size += rsp_sub;
//   }

//   // index values based on rbp
//   size_t rbp_offset = 0;
//   if (sub->ret) {
//     rbp_offset -= sizeof(x86_64_value);
//     hashset_cg_x86_64_map_value_insert(
//         ctx->sub_map_values, cg_x86_64_map_value_new(sub->ret, rbp_offset));
//   }

//   for (list_mir_value_it it = list_mir_value_begin(sub->args); !it.end(&it);
//        it.next(&it)) {
//     rbp_offset -= sizeof(x86_64_value);
//     mir_value *value = it.get(&it);
//     hashset_cg_x86_64_map_value_insert(
//         ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
//   }

//   for (list_mir_value_it it = list_mir_value_begin(sub->vars); !it.end(&it);
//        it.next(&it)) {
//     rbp_offset -= sizeof(x86_64_value);
//     mir_value *value = it.get(&it);
//     hashset_cg_x86_64_map_value_insert(
//         ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
//   }

//   for (list_mir_value_it it = list_mir_value_begin(sub->tmps); !it.end(&it);
//        it.next(&it)) {
//     rbp_offset -= sizeof(x86_64_value);
//     mir_value *value = it.get(&it);
//     hashset_cg_x86_64_map_value_insert(
//         ctx->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
//   }
// }

// // copy args to allocated stack
// static void ctx_sub_args_prologue(cg_x86_64_ctx *ctx, mir_subroutine *sub) {
//   list_cg_x86_64_op   *operands;
//   cg_x86_64_map_value *cg;
//   uint64_t             offset = ctx->sub_rbp_offset;

//   list_mir_value_it it = list_mir_value_begin(sub->args);

//   if (it.end(&it)) {
//     return;
//   }
//   cg = ctx_sub_value_find(ctx, it.get(&it));
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands)); it.next(&it);

//   if (it.end(&it)) {
//     return;
//   }
//   cg = ctx_sub_value_find(ctx, it.get(&it));
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands)); it.next(&it);

//   if (it.end(&it)) {
//     return;
//   }
//   cg = ctx_sub_value_find(ctx, it.get(&it));
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands)); it.next(&it);

//   if (it.end(&it)) {
//     return;
//   }
//   cg = ctx_sub_value_find(ctx, it.get(&it));
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands)); it.next(&it);

//   if (it.end(&it)) {
//     return;
//   }
//   cg = ctx_sub_value_find(ctx, it.get(&it));
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands)); it.next(&it);

//   if (it.end(&it)) {
//     return;
//   }
//   cg = ctx_sub_value_find(ctx, it.get(&it));
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg->rbp_offset, CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands)); it.next(&it);

//   while (!it.end(&it)) {
//     cg = ctx_sub_value_find(ctx, it.get(&it));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_base(offset,
//     CG_X86_64_REG_RBP)); cg_op_push_back(operands,
//     cg_x86_64_op_new_register(CG_X86_64_REG_RAX)); ctx_text_push_back(ctx,
//     cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_base(cg->rbp_offset,
//                     CG_X86_64_REG_RBP));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands)); it.next(&it); offset += sizeof(x86_64_value);
//   }
// }

// void ctx_build_sub(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                    size_t debug_sub_id) {
//   ctx->sub_stack_size = X86_64_ALIGN_REG_64;
//   ctx->sub_map_values = hashset_cg_x86_64_map_value_new();

//   ctx_text_push_back(
//       ctx, cg_x86_64_label_new(ctx_sub_label(ctx, sub),
//       CG_X86_64_LABEL_INSTR));

//   ctx_text_push_back(ctx,
//                      cg_x86_64_label_new(ctx_debug_sub_start(ctx,
//                      debug_sub_id),
//                                          CG_X86_64_LABEL_INSTR));

//   ctx_sub_prologue(ctx);

//   ctx_sub_values_prologue(ctx, sub);
//   ctx_sub_args_prologue(ctx, sub);

//   ctx_text_push_back(
//       ctx, cg_x86_64_label_new(ctx_debug_sub_prologue_end(ctx, debug_sub_id),
//                                CG_X86_64_LABEL_INSTR));

//   for (list_mir_bb_it it = list_mir_bb_begin(sub->bbs); !it.end(&it);
//        it.next(&it)) {
//     ctx_build_block(ctx, sub, it.get(&it));
//   }

//   int exit = ctx_is_exit(ctx, sub);

//   ctx_text_push_back(
//       ctx, cg_x86_64_label_new(ctx_debug_sub_epilogue_start(ctx,
//       debug_sub_id),
//                                CG_X86_64_LABEL_INSTR));

//   ctx_sub_epilogue(ctx, sub, !exit);
//   if (exit) {
//     ctx_sub_exit(ctx);
//   }

//   ctx_text_push_back(ctx,
//                      cg_x86_64_label_new(ctx_debug_sub_end(ctx,
//                      debug_sub_id),
//                                          CG_X86_64_LABEL_INSTR));

//   // add subroutine to debug info
//   ctx_debug_add_sub(ctx, sub, debug_sub_id);

//   hashset_cg_x86_64_map_value_free(ctx->sub_map_values);
// }
