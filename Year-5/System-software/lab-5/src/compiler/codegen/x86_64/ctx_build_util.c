// #include "ctx_build.h"

// #include "util/log.h"
// #include "util/macro.h"
// #include "x86_64_core/meta.h"
// #include "x86_64_core/type.h"
// #include <string.h>

// void cg_op_push_back(list_cg_x86_64_op *operands, cg_x86_64_op *operand) {
//   list_cg_x86_64_op_push_back(operands, operand);
// }

// cg_x86_64_op *cg_mode_new_direct_symbol(x86_64_symbol_enum kind) {
//   return cg_x86_64_op_new_direct(strdup(x86_64_symbol_enum_str(kind)));
// }

// size_t cg_with_align(size_t size, size_t align) {
//   return ((size - 1) / align + 1) * align;
// }

// void cg_units_push_back(list_cg_x86_64_unit *to, void *from) {
//   list_cg_x86_64_unit_push_back(to, from);
// }

// void cg_units_push_front(list_cg_x86_64_unit *to, void *from) {
//   list_cg_x86_64_unit_push_front(to, from);
// }

// void cg_units_extend_move(list_cg_x86_64_unit *to, list_cg_x86_64_unit *from)
// {
//   while (!list_cg_x86_64_unit_empty(from)) {
//     list_cg_x86_64_unit_push_back(to, list_cg_x86_64_unit_pop_front(from));
//   }
//   list_cg_x86_64_unit_free(from);
// }

// x86_64_type *cg_type_to_x86_64(type_base *type) {
//   switch (type->type) {
//   case TYPE_BOOL:
//     return x86_64_type_new_simple(X86_64_TYPE_BOOL);
//     break;
//   case TYPE_BYTE:
//     return x86_64_type_new_simple(X86_64_TYPE_BYTE);
//     break;
//   case TYPE_INT:
//     return x86_64_type_new_simple(X86_64_TYPE_INT);
//     break;
//   case TYPE_UINT:
//     return x86_64_type_new_simple(X86_64_TYPE_UINT);
//     break;
//   case TYPE_LONG:
//     return x86_64_type_new_simple(X86_64_TYPE_LONG);
//     break;
//   case TYPE_ULONG:
//     return x86_64_type_new_simple(X86_64_TYPE_ULONG);
//     break;
//   case TYPE_CHAR:
//     return x86_64_type_new_simple(X86_64_TYPE_CHAR);
//     break;
//   case TYPE_STRING:
//     return x86_64_type_new_simple(X86_64_TYPE_STRING);
//     break;
//   case TYPE_VOID:
//     return x86_64_type_new_simple(X86_64_TYPE_VOID);
//     break;
//   // NOTE: metadata is NULL
//   // TODO: don't recreate after each wrap
//   case TYPE_ARRAY: {
//     x86_64_type *elem_type = cg_type_to_x86_64(((type_array
//     *)type)->elem_type); x86_64_type *complex =
//         x86_64_type_new_complex(X86_64_TYPE_ARRAY, elem_type, NULL);
//     x86_64_type_free(elem_type);
//     return complex;
//   }
//   case TYPE_CLASS:
//   case TYPE_CALLABLE:
//     error("type %d is not implemented, %p", type->type, type);
//     return NULL;
//   default:
//     error("unknown type %d %p", type->type, type);
//     return NULL;
//   }
// }

// cg_x86_64_map_value *ctx_sub_value_find(cg_x86_64_ctx *ctx, mir_value *value)
// {
//   hashset_cg_x86_64_map_value_it it;

//   it = hashset_cg_x86_64_map_value_find(
//       ctx->sub_map_values,
//       &(cg_x86_64_map_value){.value_ref = (mir_value *)value});

//   if (it.end(&it)) {
//     error("value %p is not in sub_map_values", value);
//     return NULL;
//   }

//   return it.get(&it);
// }

// // requires hashset to be filled
// int ctx_in_globals(cg_x86_64_ctx *ctx, const mir_node_base *base) {
//   hashset_cg_x86_64_mir_ref_it it =
//       hashset_cg_x86_64_mir_ref_find(ctx->globls, (mir_node_base *)base);
//   return !it.end(&it);
// }

// int ctx_is_exit(cg_x86_64_ctx *ctx, const mir_subroutine *sub) {
//   UNUSED(ctx);
//   return !strcmp(sub->name, "main");
// }

// void ctx_type_serialize(cg_x86_64_ctx *ctx, x86_64_type *type) {
//   if (!type) {
//     return;
//   }
//   size_t   sz = sizeof(x86_64_type) + sizeof(type->entries[0]) * type->count;
//   uint8_t *data = MALLOCN(uint8_t, sz);
//   memcpy(data, type, sz);
//   ctx_data_push_back(ctx, cg_x86_64_data_new_bytes(sz, data));
// }

// void ctx_stack_release(cg_x86_64_ctx *ctx, size_t stack_size) {
//   list_cg_x86_64_op *operands;
//   if (stack_size) {
//     operands = list_cg_x86_64_op_new();
//     cg_op_push_back(operands, cg_x86_64_op_new_immediate(stack_size));
//     cg_op_push_back(operands, cg_x86_64_op_new_register(CG_X86_64_REG_RSP));
//     ctx_text_push_back(ctx, cg_x86_64_instr_new(CG_X86_64_MNEM_ADDQ,
//     operands)); ctx->sub_stack_size -= stack_size;
//   }
// }

// void ctx_add_error(cg_x86_64_ctx *ctx, exception_subtype_cg subtype,
//                    const char *format, ...) {
//   va_list args;
//   va_start(args, format);
//   list_exception_push_back(
//       ctx->exceptions,
//       exception_new_v(EXCEPTION_LEVEL_ERROR, EXCEPTION_CG, subtype, NULL, 0,
//       0,
//                       exception_subtype_cg_str(subtype), format, args));
//   va_end(args);
// }
