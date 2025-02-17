// #include "compiler/mir/str.h"
// #include "ctx_build.h"

// #include "util/log.h"
// #include "x86_64_core/value.h"
// #include <string.h>

// #define cg_mir_node_kind_assign(node, node_kind, assign, assign_kind)          \
//   do {                                                                         \
//     if (node_kind != assign_kind) {                                            \
//       error("expected arg %d, got %d %p", assign_kind, node_kind, node);       \
//       return;                                                                  \
//     }                                                                          \
//     assign = (void *)node;                                                     \
//   } while (0)

// static void ctx_value_drop(cg_x86_64_ctx *ctx, cg_x86_64_map_value *cg_lsv) {
//   list_cg_x86_64_op *operands;

//   // leaq lsv_off(%rbp), rdi
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg_lsv->rbp_offset,
//                   CG_X86_64_REG_RBP));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ,
//   operands));

//   // call __x86_64_drop
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands,
//                   cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_DROP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//   operands));
// }

// static uint64_t ctx_args_prepare(cg_x86_64_ctx *ctx, int variadic,
//                                  const list_cg_x86_64_map_value *values,
//                                  int64_t                         arg_offset)
//                                  {
//   list_cg_x86_64_op          *operands;
//   cg_x86_64_map_value        *cg;
//   size_t                      aligned_rsp_size = 0;
//   size_t                      align;
//   size_t                      prev_stack_size = ctx->sub_stack_size;
//   list_cg_x86_64_unit        *texts = list_cg_x86_64_unit_new();
//   list_cg_x86_64_map_value_it it = list_cg_x86_64_map_value_begin(values);
//   list_cg_x86_64_op          *v_ops;

//   if (arg_offset < 0) {
//     arg_offset = 0;
//   }

//   if (variadic) {
//     v_ops = list_cg_x86_64_op_new();
//     cg_op_push_back(v_ops, cg_x86_64_op_new_immediate(0));
//   }

//   switch (arg_offset) {
//   case 0:
//     if (END(it)) {
//       if (variadic) {
//         cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//         list_cg_x86_64_unit_push_back(
//             texts,
//             (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//             v_ops));
//       }
//       variadic = 0;
//       break;
//     }
//     cg = GET(it);
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_base(cg->rbp_offset,
//                     CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                              CG_X86_64_MNEM_MOVQ, operands));
//     NEXT(it);
//     __attribute__((fallthrough));
//   case 1:
//     if (END(it)) {
//       if (variadic) {
//         cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
//         list_cg_x86_64_unit_push_back(
//             texts,
//             (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//             v_ops));
//       }
//       variadic = 0;
//       break;
//     }
//     cg = GET(it);
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_base(cg->rbp_offset,
//                     CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
//     list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                              CG_X86_64_MNEM_MOVQ, operands));
//     NEXT(it);
//     __attribute__((fallthrough));
//   case 2:
//     if (END(it)) {
//       if (variadic) {
//         cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
//         list_cg_x86_64_unit_push_back(
//             texts,
//             (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//             v_ops));
//       }
//       variadic = 0;
//       break;
//     }
//     cg = GET(it);
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_base(cg->rbp_offset,
//                     CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
//     list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                              CG_X86_64_MNEM_MOVQ, operands));
//     NEXT(it);
//     __attribute__((fallthrough));
//   case 3:
//     if (END(it)) {
//       if (variadic) {
//         cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
//         list_cg_x86_64_unit_push_back(
//             texts,
//             (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//             v_ops));
//       }
//       variadic = 0;
//       break;
//     }
//     cg = GET(it);
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_base(cg->rbp_offset,
//                     CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
//     list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                              CG_X86_64_MNEM_MOVQ, operands));
//     NEXT(it);
//     __attribute__((fallthrough));
//   case 4:
//     if (END(it)) {
//       if (variadic) {
//         cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
//         list_cg_x86_64_unit_push_back(
//             texts,
//             (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//             v_ops));
//       }
//       variadic = 0;
//       break;
//     }
//     cg = GET(it);
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_base(cg->rbp_offset,
//                     CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
//     list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                              CG_X86_64_MNEM_MOVQ, operands));
//     NEXT(it);
//     __attribute__((fallthrough));
//   case 5:
//     if (END(it)) {
//       if (variadic) {
//         cg_op_push_back(v_ops, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
//         list_cg_x86_64_unit_push_back(
//             texts,
//             (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//             v_ops));
//       }
//       variadic = 0;
//       break;
//     }
//     cg = GET(it);
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_base(cg->rbp_offset,
//                     CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
//     list_cg_x86_64_unit_push_back(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                              CG_X86_64_MNEM_MOVQ, operands));
//     NEXT(it);
//     __attribute__((fallthrough));
//   case 6:
//     while (!END(it)) {
//       cg = GET(it);
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands,
//                       cg_x86_64_op_new_base(cg->rbp_offset,
//                       CG_X86_64_REG_RBP));
//       list_cg_x86_64_unit_push_front(
//           texts, (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_PUSHQ,
//                                                        operands));
//       ctx->sub_stack_size += sizeof(x86_64_value);
//       NEXT(it);
//     }
//     __attribute__((fallthrough));
//   default:
//     break;
//   }

//   if (variadic) {
//     list_cg_x86_64_unit_push_front(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                               CG_X86_64_MNEM_PUSHQ,
//                                               operands));
//     ctx->sub_stack_size += sizeof(x86_64_value);
//   }

//   // stack is pushed by who executes this command, so no need to no anything
//   aligned_rsp_size = cg_with_align(ctx->sub_stack_size, X86_64_ALIGN_CALL);
//   align = aligned_rsp_size - ctx->sub_stack_size;

//   if (align) {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(align));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
//     list_cg_x86_64_unit_push_front(texts, (cg_x86_64_unit
//     *)cg_x86_64_instr_new(
//                                               CG_X86_64_MNEM_SUBQ,
//                                               operands));
//     ctx->sub_stack_size += align;
//   }

//   ctx_text_push_back_many(ctx, texts);

//   return ctx->sub_stack_size - prev_stack_size;
// }

// static void ctx_stmt_store_lit(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                                mir_value *lsv, mir_lit *rsv) {
//   list_cg_x86_64_op   *operands;
//   cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, lsv);

//   hir_type_enum rsv_type = rsv->lit->type->type;

//   // init
//   if (rsv_type == TYPE_VOID) {
//     // leaq lsv_off(%rbp), rdi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(
//         operands, cg_x86_64_op_new_base(cg_lsv->rbp_offset,
//         CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ,
//     operands));

//     // call __x86_64_init
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INIT));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands)); return;
//   }

//   ctx_value_drop(ctx, cg_lsv);

//   switch (rsv_type) {
//   case TYPE_BOOL: {
//     uint8_t value = rsv->lit->value.v_bool;

//     // movb $bool, %dil
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB,
//     operands));

//     // call __x86_64_value_bool
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BOOL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//   } break;
//   case TYPE_BYTE: {
//     uint8_t value = rsv->lit->value.v_ulong;

//     // movb $byte, %dil
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB,
//     operands));

//     // call __x86_64_value_byte
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BYTE));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//   } break;
//   case TYPE_INT: {
//     int32_t value = rsv->lit->value.v_long;

//     // movb $int, %edi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL,
//     operands));

//     // call __x86_64_value_int
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INT));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//   } break;
//   case TYPE_UINT: {
//     uint32_t value = rsv->lit->value.v_ulong;

//     // movb $uint, %edi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL,
//     operands));

//     // call __x86_64_value_uint
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_UINT));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_LONG: {
//     int64_t value = rsv->lit->value.v_long;

//     // movb $long, %rdi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));

//     // call __x86_64_value_long
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_LONG));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_ULONG: {
//     uint64_t value = rsv->lit->value.v_ulong;

//     // movb $ulong, %rdi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));

//     // call __x86_64_value_ulong
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ULONG));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//   } break;
//   case TYPE_CHAR: {
//     char   *chr = rsv->lit->value.v_char;
//     uint8_t value = chr[0];

//     // movb $char, %dil
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(value));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB,
//     operands));

//     // call __x86_64_value_char
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_CHAR));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//   } break;
//   case TYPE_STRING: {
//     char *str = strdup(rsv->lit->value.v_str);

//     // add to data region
//     ctx_data_push_back(ctx, cg_x86_64_label_new(ctx_data_label(ctx, sub,
//     lsv),
//                                                 CG_X86_64_LABEL_DATA));
//     ctx_data_push_back(ctx, cg_x86_64_data_new_ascii((uint8_t *)str));

//     // movb data_label, %rdi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_direct(ctx_data_label(ctx, sub, lsv)));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ,
//     operands));

//     // call __x86_64_value_string
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_STRING));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//   } break;
//   default:
//     error("unexpected lit type %s %p", type_enum_str(rsv_type), rsv);
//     return;
//   }

//   // store value to stack
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg_lsv->rbp_offset,
//                   CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));
// }

// static void ctx_stmt_store_assign(cg_x86_64_ctx *ctx, mir_value *lsv,
//                                   mir_value *rsv) {
//   cg_x86_64_map_value *cg_lsv, *cg_rsv;
//   list_cg_x86_64_op   *operands;

//   cg_lsv = ctx_sub_value_find(ctx, lsv);
//   cg_rsv = ctx_sub_value_find(ctx, rsv);

//   // leaq lsv_off(%rbp), %rdi
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg_lsv->rbp_offset,
//                   CG_X86_64_REG_RBP));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ,
//   operands));

//   // movq rsv_off(%rbp), %rsi
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg_rsv->rbp_offset,
//                   CG_X86_64_REG_RBP));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));

//   // call __x86_64_assign
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_ASSIGN));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//   operands));
// }

// static void ctx_stmt_builtin_l_value(cg_x86_64_ctx     *ctx,
//                                      x86_64_symbol_enum symbol, int variadic,
//                                      const list_cg_x86_64_map_value *values)
//                                      {
//   list_cg_x86_64_op *operands;
//   size_t             stack_size;

//   stack_size = ctx_args_prepare(ctx, variadic, values, 0);

//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_mode_new_direct_symbol(symbol));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//   operands));

//   ctx_stack_release(ctx, stack_size);
// }

// static void ctx_stmt_builtin_f_value(cg_x86_64_ctx     *ctx,
//                                      x86_64_symbol_enum symbol, int variadic,
//                                      size_t n, ...) {
//   va_list args;
//   va_start(args, n);

//   list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();
//   while (n--) {
//     list_cg_x86_64_map_value_push_back(values,
//                                        va_arg(args, cg_x86_64_map_value *));
//   }
//   ctx_stmt_builtin_l_value(ctx, symbol, variadic, values);

//   while (!list_cg_x86_64_map_value_empty(values)) {
//     list_cg_x86_64_map_value_pop_front(values);
//   }
//   list_cg_x86_64_map_value_free(values);
// }

// static void ctx_stmt_builtin_2(cg_x86_64_ctx *ctx, mir_builtin *builtin,
//                                x86_64_symbol_enum symbol) {
//   if (list_mir_arg_size(builtin->args) != 2) {
//     error("expected 2 args %p", builtin);
//     return;
//   }

//   union {
//     mir_node_base       *node;
//     mir_value           *value;
//     cg_x86_64_map_value *cg;
//   } a1, a2;

//   list_mir_arg_it it = list_mir_arg_begin(builtin->args);
//   a1.node = GET(it);
//   NEXT(it);
//   a2.node = GET(it);

//   cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);
//   cg_mir_node_kind_assign(a2.node, a2.node->kind, a2.value, MIR_NODE_VALUE);

//   a1.cg = ctx_sub_value_find(ctx, a1.value);
//   a2.cg = ctx_sub_value_find(ctx, a2.value);

//   ctx_stmt_builtin_f_value(ctx, symbol, 0, 2, a1.cg, a2.cg);
// }

// void ctx_stmt_builtin_1(cg_x86_64_ctx *ctx, mir_builtin *builtin,
//                         x86_64_symbol_enum symbol) {
//   if (list_mir_arg_size(builtin->args) != 1) {
//     error("expected 1 args %p", builtin);
//     return;
//   }

//   union {
//     mir_node_base       *node;
//     mir_value           *value;
//     cg_x86_64_map_value *cg;
//   } a1;

//   list_mir_arg_it it = list_mir_arg_begin(builtin->args);
//   a1.node = GET(it);

//   cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);

//   a1.cg = ctx_sub_value_find(ctx, a1.value);

//   ctx_stmt_builtin_f_value(ctx, symbol, 0, 1, a1.cg);
// }

// void ctx_stmt_builtin_greater(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
//   if (list_mir_arg_size(builtin->args) != 2) {
//     error("expected 2 args %p", builtin);
//     return;
//   }

//   union {
//     mir_node_base       *node;
//     mir_value           *value;
//     cg_x86_64_map_value *cg;
//   } a1, a2;

//   list_mir_arg_it it = list_mir_arg_begin(builtin->args);
//   a1.node = GET(it);
//   NEXT(it);
//   a2.node = GET(it);

//   cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);
//   cg_mir_node_kind_assign(a2.node, a2.node->kind, a2.value, MIR_NODE_VALUE);

//   a1.cg = ctx_sub_value_find(ctx, a1.value);
//   a2.cg = ctx_sub_value_find(ctx, a2.value);

//   ctx_stmt_builtin_f_value(ctx, X86_64_SYMBOL_LESS, 0, 2, a2.cg, a1.cg);
// }

// void ctx_stmt_builtin_greater_eq(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
//   if (list_mir_arg_size(builtin->args) != 2) {
//     error("expected 2 args %p", builtin);
//     return;
//   }

//   union {
//     mir_node_base       *node;
//     mir_value           *value;
//     cg_x86_64_map_value *cg;
//   } a1, a2;

//   list_mir_arg_it it = list_mir_arg_begin(builtin->args);
//   a1.node = GET(it);
//   NEXT(it);
//   a2.node = GET(it);

//   cg_mir_node_kind_assign(a1.node, a1.node->kind, a1.value, MIR_NODE_VALUE);
//   cg_mir_node_kind_assign(a2.node, a2.node->kind, a2.value, MIR_NODE_VALUE);

//   a1.cg = ctx_sub_value_find(ctx, a1.value);
//   a2.cg = ctx_sub_value_find(ctx, a2.value);

//   ctx_stmt_builtin_f_value(ctx, X86_64_SYMBOL_LESS_EQ, 0, 2, a2.cg, a1.cg);
// }

// static void ctx_stmt_builtin_call(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
//   if (list_mir_arg_size(builtin->args) == 0) {
//     error("expected at least 1 arg (nothing to be called) %p", builtin);
//     return;
//   }

//   union {
//     mir_node_base      *node;
//     mir_subroutine     *sub;
//     mir_subroutine_ext *ext;
//   } fun;

//   list_mir_arg_it it = list_mir_arg_begin(builtin->args);
//   fun.node = GET(it);

//   switch (fun.node->kind) {
//   case MIR_NODE_SUBROUTINE: {
//     list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();

//     union {
//       mir_node_base       *node;
//       mir_value           *value;
//       cg_x86_64_map_value *cg;
//     } arg;

//     while (NEXT(it)) {
//       arg.node = GET(it);

//       if (arg.node->kind != MIR_NODE_VALUE) {
//         error("expected %d as argument, got %d %p", MIR_NODE_VALUE,
//               arg.node->kind, arg.node);
//         goto release;
//       }
//       arg.value = (mir_value *)arg.node;
//       arg.cg = ctx_sub_value_find(ctx, arg.value);

//       list_cg_x86_64_map_value_push_back(values, arg.cg);
//     }

//     size_t stack_size = ctx_args_prepare(ctx, 0, values, 0);

//     // call func
//     list_cg_x86_64_op *operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_direct(ctx_sub_label(ctx, fun.sub)));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//     ctx_stack_release(ctx, stack_size);

//   release:
//     while (!list_cg_x86_64_map_value_empty(values)) {
//       list_cg_x86_64_map_value_pop_front(values);
//     }
//     list_cg_x86_64_map_value_free(values);
//   } break;
//     // unwrap arguments based on type
//     // if argument pointer -> pass __x86_64_unwrap_array
//     // if result void -> __x86_64_value_void()
//     // if result pointer -> __x86_64_value_array with 1 element
//   case MIR_NODE_SUBROUTINE_EXTERNAL: {
//     size_t type_sz = list_type_size(fun.ext->args);
//     size_t args_sz = list_mir_arg_size(builtin->args) - 1;
//     // TODO: move validation to previous stages
//     if (type_sz != args_sz) {
//       ctx_add_error(ctx, EXCEPTION_CG_EXTERNAL_SUBROUTINE_ARGC,
//                     "%s call expected %zu args, got %zu", fun.ext->name,
//                     type_sz, args_sz);
//       break;
//     }
//     list_cg_x86_64_op *operands;
//     list_type_it       t_it = list_type_begin(fun.ext->args);
//     size_t             argc = 0;
//     size_t             prev_stack = ctx->sub_stack_size;

//     list_cg_x86_64_unit *text_units = list_cg_x86_64_unit_new();

//     // NOTE:
//     // 1. unwrap arguments (pushed to front, so reverse order)
//     // 2. stack here isn't aligned, but doesn't matter (too lazy)
//     for (NEXT(it); !END(it); NEXT(it), NEXT(t_it), ++argc)
//     {
//       mir_node_base *node = GET(it);
//       type_base     *type = GET(t_it);

//       if (node->kind != MIR_NODE_VALUE) {
//         error("expected VALUE(%d) kind, got %d", MIR_NODE_VALUE, node->kind);
//         break;
//       }
//       mir_value           *value = (mir_value *)node;
//       cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);

//       // pushq %rax (arguments are aligned before push)
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
//       list_cg_x86_64_unit_push_front(text_units,
//                                      (cg_x86_64_unit *)cg_x86_64_instr_new(
//                                          CG_X86_64_MNEM_PUSHQ, operands));

//       // call <__unwrap_*>
//       operands = list_cg_x86_64_op_new();
//       switch (type->type) {
//       case TYPE_BOOL:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_BOOL));
//         break;
//       case TYPE_INT:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_INT));
//         break;
//       case TYPE_UINT:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_UINT));
//         break;
//       case TYPE_LONG:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_LONG));
//         break;
//       case TYPE_ULONG:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_ULONG));
//         break;
//       case TYPE_CHAR:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_CHAR));
//         break;
//       case TYPE_STRING:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_STRING));
//         break;
//       case TYPE_ARRAY:
//         cg_op_push_back(operands,
//                         cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_ARRAY));
//         break;
//       default:
//         error("unsupported type %d %p", type->type, builtin);
//         break;
//       }
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//           operands));

//       // mov val, %rdi
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands, cg_x86_64_op_new_base(cg_value->rbp_offset,
//                                                       CG_X86_64_REG_RBP));
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//           operands));
//     }

//     // calculate stack offset (at max 6 arguments will be popped)
//     ctx->sub_stack_size += (argc > 6 ? argc - 6 : 0) * X86_64_ALIGN_MEMORY;

//     size_t align = cg_with_align(ctx->sub_stack_size, X86_64_ALIGN_CALL) -
//                    ctx->sub_stack_size;

//     // align stack before pushes
//     if (align) {
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands, cg_x86_64_op_new_immediate(align));
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RSP)); ctx_text_push_back(ctx,
//                          cg_x86_64_instr_new(CG_X86_64_MNEM_SUBQ, operands));
//       ctx->sub_stack_size += align;
//     }

//     ctx_text_push_back_many(ctx, text_units);

//     // pop 6 arguments from stack (in reverse order)
//     text_units = list_cg_x86_64_unit_new();
//     switch (argc > 6 ? 6 : argc) {
//     case 6:
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R9));
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ,
//           operands));
//       __attribute__((fallthrough));
//     case 5:
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_R8));
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ,
//           operands));
//       __attribute__((fallthrough));
//     case 4:
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RCX));
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ,
//           operands));
//       __attribute__((fallthrough));
//     case 3:
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RDX));
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ,
//           operands));
//       __attribute__((fallthrough));
//     case 2:
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ,
//           operands));
//       __attribute__((fallthrough));
//     case 1:
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//       list_cg_x86_64_unit_push_front(
//           text_units,
//           (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ,
//           operands));
//       __attribute__((fallthrough));
//     default:
//       break;
//     }
//     ctx_text_push_back_many(ctx, text_units);

//     // call external function
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_direct(ctx_ext_label(ctx, fun.ext)));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//     // movq %rax, %rdi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));

//     // call __cg_x86_64_value for something (only one argument)
//     operands = list_cg_x86_64_op_new();
//     switch (fun.ext->ret->type) {
//     case TYPE_BOOL:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BOOL));
//       break;
//     case TYPE_BYTE:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BYTE));
//       break;
//     case TYPE_INT:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INT));
//       break;
//     case TYPE_UINT:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_UINT));
//       break;
//     case TYPE_LONG:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_LONG));
//       break;
//     case TYPE_ULONG:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ULONG));
//       break;
//     case TYPE_CHAR:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_CHAR));
//       break;
//     case TYPE_STRING:
//       cg_op_push_back(
//           operands,
//           cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_STRING_MOVE));
//       break;
//     case TYPE_VOID:
//       cg_op_push_back(operands,
//                       cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_VOID));
//       break;
//     default:
//       error("unexpected ret type %d", fun.ext->ret->type);
//       break;
//     }
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//     // restore stack
//     if (ctx->sub_stack_size - prev_stack) {
//       operands = list_cg_x86_64_op_new();
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_immediate(ctx->sub_stack_size -
//                                                            prev_stack));
//       cg_op_push_back(operands,
//       cg_x86_64_op_new_register(CG_X86_64_REG_RSP)); ctx_text_push_back(ctx,
//                          cg_x86_64_instr_new(CG_X86_64_MNEM_ADDQ, operands));
//     }

//   } break;
//   default:
//     ctx_add_error(ctx, EXCEPTION_CG_IMPLICIT_CALLABLE,
//                   "expected subroutine, got %s(%d)",
//                   mir_node_enum_str(fun.node->kind), fun.node->kind);
//     return;
//   }
// }

// void ctx_stmt_builtin_index(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
//   list_cg_x86_64_op        *operands;
//   list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();

//   for (list_mir_arg_it it = list_mir_arg_begin(builtin->args); !END(it);
//        NEXT(it)) {
//     mir_node_base *node = GET(it);
//     if (node->kind != MIR_NODE_VALUE) {
//       ctx_add_error(ctx, EXCEPTION_CG_IMPLICIT_CALLABLE, "expected %s, got
//       %s",
//                     mir_node_enum_str(MIR_NODE_VALUE),
//                     mir_node_enum_str(node->kind));
//       goto release;
//     }
//     mir_value           *value = (mir_value *)node;
//     cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);
//     list_cg_x86_64_map_value_push_back(values, cg_value);
//   }

//   size_t stack_size = ctx_args_prepare(ctx, 1, values, 0);

//   // call func
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands,
//   cg_mode_new_direct_symbol(X86_64_SYMBOL_INDEX_GET));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//   operands));

//   ctx_stack_release(ctx, stack_size);

// release:
//   while (!list_cg_x86_64_map_value_empty(values)) {
//     list_cg_x86_64_map_value_pop_front(values);
//   }
//   list_cg_x86_64_map_value_free(values);
// }

// // TODO: complex types
// void ctx_stmt_builtin_make(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                            mir_value *lsv, mir_builtin *builtin) {
//   list_cg_x86_64_op *operands;

//   hir_type_enum spec_type = builtin->spec->type;

//   switch (spec_type) {
//   case TYPE_BOOL: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BOOL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_BYTE: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_BYTE));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_INT: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_INT));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_UINT: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_EDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVL,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_UINT));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_LONG: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_LONG));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_ULONG: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ULONG));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_CHAR: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_DIL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVB,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_CHAR));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_STRING: {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(0));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));

//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_STRING));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));
//   } break;
//   case TYPE_ARRAY: {
//     type_base *elem_type = ((type_array *)builtin->spec)->elem_type;

//     x86_64_type *type = cg_type_to_x86_64(elem_type);
//     ctx_data_push_back(ctx, cg_x86_64_label_new(ctx_data_label(ctx, sub,
//     lsv),
//                                                 CG_X86_64_LABEL_DATA));
//     ctx_type_serialize(ctx, type);
//     x86_64_type_free(type);

//     // leaq .Ldata, %rdi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_direct(ctx_data_label(ctx, sub, lsv)));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ,
//     operands));

//     // mov variadic arguments
//     list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();
//     for (list_mir_arg_it it = list_mir_arg_begin(builtin->args);
//     !END(it);
//          NEXT(it)) {
//       mir_node_base *node = GET(it);
//       if (node->kind != MIR_NODE_VALUE) {
//         error("expected VALUE(%d) kind, got %d", MIR_NODE_VALUE, node->kind);
//         goto release;
//       }
//       mir_value           *value = (mir_value *)node;
//       cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);
//       list_cg_x86_64_map_value_push_back(values, cg_value);
//     }
//     size_t stack_size = ctx_args_prepare(ctx, 1, values, 1);

//     // call func
//     list_cg_x86_64_op *operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_VALUE_ARRAY));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//     ctx_stack_release(ctx, stack_size);

//   release:
//     while (!list_cg_x86_64_map_value_empty(values)) {
//       list_cg_x86_64_map_value_pop_front(values);
//     }
//     list_cg_x86_64_map_value_free(values);
//   } break;
//   default:
//     error("unsupported new type %s %p", type_enum_str(spec_type), builtin);
//     return;
//   }
// }

// void ctx_stmt_builtin_cast(cg_x86_64_ctx *ctx, mir_builtin *builtin) {
//   if (list_mir_arg_size(builtin->args) != 1) {
//     error("expected 1 arg %p", builtin);
//     return;
//   }

//   list_cg_x86_64_op *operands;
//   union {
//     mir_node_base       *node;
//     mir_value           *value;
//     cg_x86_64_map_value *cg;
//   } arg;

//   {
//     arg.node = list_mir_arg_front(builtin->args);
//     if (arg.node->kind != MIR_NODE_VALUE) {
//       error("expected %d as argument, got %d %p", MIR_NODE_VALUE,
//             arg.node->kind, arg.node);
//       return;
//     }

//     arg.value = (mir_value *)arg.node;
//     arg.cg = ctx_sub_value_find(ctx, arg.value);
//   }

//   hir_type_enum        spec_type = builtin->spec->type;
//   x86_64_type_enum type;
//   switch (spec_type) {
//   case TYPE_BOOL:
//     type = X86_64_TYPE_BOOL;
//     break;
//   case TYPE_BYTE:
//     type = X86_64_TYPE_BYTE;
//     break;
//   case TYPE_INT:
//     type = X86_64_TYPE_INT;
//     break;
//   case TYPE_UINT:
//     type = X86_64_TYPE_UINT;
//     break;
//   case TYPE_LONG:
//     type = X86_64_TYPE_LONG;
//     break;
//   case TYPE_ULONG:
//     type = X86_64_TYPE_ULONG;
//     break;
//   case TYPE_CHAR:
//     type = X86_64_TYPE_CHAR;
//     break;
//   case TYPE_STRING:
//     type = X86_64_TYPE_STRING;
//     break;
//   case TYPE_VOID:
//     type = X86_64_TYPE_VOID;
//     break;
//   case TYPE_ARRAY:
//     type = X86_64_TYPE_ARRAY;
//     break;
//   default:
//     error("unsupported cast type %s is not supported",
//           type_enum_str(spec_type));
//     type = X86_64_TYPE_VOID;
//   }

//   uint64_t type_i = type;

//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(arg.cg->rbp_offset,
//                   CG_X86_64_REG_RBP));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));

//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_immediate(type_i));
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSI));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));

//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_mode_new_direct_symbol(X86_64_SYMBOL_CAST));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//   operands));
// }

// void ctx_stmt_store_builtin(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                             mir_value *lsv, mir_builtin *rsv) {
//   cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, lsv);

//   ctx_value_drop(ctx, cg_lsv);

//   switch (rsv->kind) {
//   case MIR_BUILTIN_PLUS:
//     ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_PLUS);
//     break;
//   case MIR_BUILTIN_MINUS:
//     ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_MINUS);
//     break;
//   case MIR_BUILTIN_LOGICAL_NOT:
//     ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_NOT);
//     break;
//   case MIR_BUILTIN_BITWISE_NOT:
//     ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_BIT_NOT);
//     break;
//   case MIR_BUILTIN_INC:
//     ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_INC);
//     break;
//   case MIR_BUILTIN_DEC:
//     ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_DEC);
//     break;
//   case MIR_BUILTIN_LOGICAL_OR:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_OR);
//     break;
//   case MIR_BUILTIN_LOGICAL_AND:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_AND);
//     break;
//   case MIR_BUILTIN_BITWISE_OR:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_OR);
//     break;
//   case MIR_BUILTIN_BITWISE_XOR:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_XOR);
//     break;
//   case MIR_BUILTIN_BITWISE_AND:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_AND);
//     break;
//   case MIR_BUILTIN_EQUALS:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_EQ);
//     break;
//   case MIR_BUILTIN_NOT_EQUALS:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_NEQ);
//     break;
//   case MIR_BUILTIN_LESS:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_LESS);
//     break;
//   case MIR_BUILTIN_LESS_EQUALS:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_LESS_EQ);
//     break;
//   case MIR_BUILTIN_GREATER:
//     ctx_stmt_builtin_greater(ctx, rsv);
//     break;
//   case MIR_BUILTIN_GREATER_EQUALS:
//     ctx_stmt_builtin_greater_eq(ctx, rsv);
//     break;
//   case MIR_BUILTIN_BITWISE_SHIFT_LEFT:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_SHL);
//     break;
//   case MIR_BUILTIN_BITWISE_SHIFT_RIGHT:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_BIT_SHR);
//     break;
//   case MIR_BUILTIN_ADD:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_ADD);
//     break;
//   case MIR_BUILTIN_SUB:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_SUB);
//     break;
//   case MIR_BUILTIN_MUL:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_MUL);
//     break;
//   case MIR_BUILTIN_DIV:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_DIV);
//     break;
//   case MIR_BUILTIN_MOD:
//     ctx_stmt_builtin_2(ctx, rsv, X86_64_SYMBOL_MOD);
//     break;
//   case MIR_BUILTIN_CALL:
//     ctx_stmt_builtin_call(ctx, rsv);
//     break;
//   case MIR_BUILTIN_INDEX:
//     ctx_stmt_builtin_index(ctx, rsv);
//     break;
//   case MIR_BUILTIN_CAST:
//     ctx_stmt_builtin_cast(ctx, rsv);
//     break;
//   case MIR_BUILTIN_MAKE:
//     ctx_stmt_builtin_make(ctx, sub, lsv, rsv);
//     break;
//   case MIR_BUILTIN_PRINT:
//     ctx_stmt_builtin_1(ctx, rsv, X86_64_SYMBOL_PRINT);
//     break;
//   default:
//     error("unexpected store builtin kind %d %p", rsv->kind, rsv);
//     return;
//   }

//   list_cg_x86_64_op *operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RAX));
//   cg_op_push_back(operands,
//                   cg_x86_64_op_new_base(cg_lsv->rbp_offset,
//                   CG_X86_64_REG_RBP));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//   operands));
// }

// void ctx_stmt_store_to_builtin(cg_x86_64_ctx *ctx, mir_builtin *builtin,
//                                mir_value *rsv) {
//   list_cg_x86_64_op        *operands;
//   list_cg_x86_64_map_value *values = list_cg_x86_64_map_value_new();

//   // from
//   cg_x86_64_map_value *cg_rsv = ctx_sub_value_find(ctx, rsv);
//   list_cg_x86_64_map_value_push_back(values, cg_rsv);

//   // to
//   for (list_mir_arg_it it = list_mir_arg_begin(builtin->args); !END(it);
//        NEXT(it)) {
//     mir_node_base *node = GET(it);
//     if (node->kind != MIR_NODE_VALUE) {
//       error("expected VALUE(%d) kind, got %d", MIR_NODE_VALUE, node->kind);
//       goto release;
//     }
//     mir_value           *value = (mir_value *)node;
//     cg_x86_64_map_value *cg_value = ctx_sub_value_find(ctx, value);
//     list_cg_x86_64_map_value_push_back(values, cg_value);
//   }

//   size_t stack_size = ctx_args_prepare(ctx, 1, values, 0);

//   // call func
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands,
//   cg_mode_new_direct_symbol(X86_64_SYMBOL_INDEX_SET));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//   operands));

//   ctx_stack_release(ctx, stack_size);

// release:
//   while (!list_cg_x86_64_map_value_empty(values)) {
//     list_cg_x86_64_map_value_pop_front(values);
//   }
//   list_cg_x86_64_map_value_free(values);
// }

// static void ctx_stmt_store(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                            mir_builtin *stmt) {
//   if (list_mir_arg_size(stmt->args) != 2) {
//     error("store requires 2 args");
//     goto release;
//   }

//   mir_node_base *first;
//   mir_node_base *second;

//   {
//     list_mir_arg_it it = list_mir_arg_begin(stmt->args);
//     first = GET(it);
//     NEXT(it);
//     second = GET(it);
//   }

//   switch (first->kind) {
//   case MIR_NODE_VALUE: {
//     switch (second->kind) {
//     case MIR_NODE_VALUE: {
//       ctx_stmt_store_assign(ctx, (mir_value *)first, (mir_value *)second);
//     } break;
//     case MIR_NODE_LIT: {
//       ctx_stmt_store_lit(ctx, sub, (mir_value *)first, (mir_lit *)second);
//     } break;
//     case MIR_NODE_BUILTIN: {
//       ctx_stmt_store_builtin(ctx, sub, (mir_value *)first,
//                              (mir_builtin *)second);
//     } break;
//     default:
//       goto unhandled;
//     }
//   } break;
//   case MIR_NODE_BUILTIN: {
//     switch (second->kind) {
//     case MIR_NODE_VALUE:
//       ctx_stmt_store_to_builtin(ctx, (mir_builtin *)first, (mir_value
//       *)second); break;
//     default:
//       goto unhandled;
//     }
//   } break;
//   default:
//     goto unhandled;
//   }

//   return;

// unhandled:
//   error("unexpected pair %d %d", first->kind, second->kind);
// release:
//   return;
// }

// static void ctx_stmt_drop(cg_x86_64_ctx *ctx, mir_builtin *stmt) {
//   union {
//     mir_node_base *node;
//     mir_value     *value;
//   } lsv;

//   if (list_mir_arg_size(stmt->args) != 1) {
//     error("expected 1 arg for drop statement");
//     return;
//   }

//   lsv.node = (mir_node_base *)list_mir_arg_front(stmt->args);

//   if (lsv.node->kind != MIR_NODE_VALUE) {
//     error("arg should be value, got %d %p", lsv.node->kind, lsv.node);
//     return;
//   }

//   cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, lsv.value);
//   ctx_value_drop(ctx, cg_lsv);
// }

// static void ctx_stmt(cg_x86_64_ctx *ctx, mir_subroutine *sub,
//                      mir_node_base *stmt) {
//   switch (stmt->kind) {
//   case MIR_NODE_BUILTIN: {
//     mir_builtin *builtin = (mir_builtin *)stmt;

//     // add debug_lines
//     mir_debug_line *debug_line = builtin->debug_line;
//     if (debug_line) {
//       uint64_t line_id = ctx->debug_lines_cnt++;
//       ctx_text_push_back(ctx, cg_x86_64_label_new(ctx_debug_line(ctx,
//       line_id),
//                                                   CG_X86_64_LABEL_INSTR));
//       ctx_debug_add_line(ctx, debug_line, line_id);
//     }

//     switch (builtin->kind) {
//     case MIR_BUILTIN_STORE: {
//       ctx_stmt_store(ctx, sub, builtin);
//     } break;
//     case MIR_BUILTIN_DROP: {
//       ctx_stmt_drop(ctx, builtin);
//     } break;
//     default:
//       error("unexpected builtin kind %d of stmt %p", builtin->kind, stmt);
//     }
//   } break;
//   default:
//     error("unexpected kind %d of stmt %p", stmt->kind, stmt);
//     break;
//   }
// }

// // no jump only on last block
// static void ctx_jump(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_jmp *jmp) {
//   list_cg_x86_64_op *operands;

//   if (!jmp) {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_x86_64_op_new_direct(ctx_sub_epilogue_label(ctx,
//                     sub)));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_JMP,
//     operands)); return;
//   }

//   // conditional
//   if (jmp->value_ref) {
//     cg_x86_64_map_value *cg_lsv = ctx_sub_value_find(ctx, jmp->value_ref);

//     // movq lsv_off(%rbp), rdi
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(
//         operands, cg_x86_64_op_new_base(cg_lsv->rbp_offset,
//         CG_X86_64_REG_RBP));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RDI));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ,
//     operands));

//     // call __x86_64_unwrap_bool
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands,
//                     cg_mode_new_direct_symbol(X86_64_SYMBOL_UNWRAP_BOOL));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_CALL,
//     operands));

//     // textb %al, %al
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_AL));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_AL));
//     ctx_text_push_back(ctx,
//                        cg_x86_64_instr_new(CG_X86_64_MNEM_TESTB, operands));

//     // je .Lje
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_direct(
//                                   ctx_block_label(ctx, sub, jmp->je_ref)));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_JNZ,
//     operands));
//   }

//   // jmp .Ljz
//   operands = list_cg_x86_64_op_new();
//   cg_op_push_back(operands, cg_x86_64_op_new_direct(
//                                 ctx_block_label(ctx, sub, jmp->jz_ref)));
//   ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_JMP, operands));
// }

// void ctx_build_block(cg_x86_64_ctx *ctx, mir_subroutine *sub, mir_bb *bb) {
//   ctx_text_push_back(ctx, cg_x86_64_label_new(ctx_block_label(ctx, sub, bb),
//                                               CG_X86_64_LABEL_INSTR));

//   for (list_mir_node_it it = list_mir_node_begin(bb->stmts); !END(it);
//        NEXT(it)) {
//     mir_node_base *stmt = GET(it);
//     ctx_stmt(ctx, sub, stmt);
//   }

//   ctx_jump(ctx, sub, bb->jmp);
// }
