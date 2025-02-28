// #include "str.h"

// const char *eir_expr_unary_enum_str(eir_expr_unary_enum type) {
//   switch (type) {
//   case EIR_EXPR_UNARY_UNKNOWN:
//     return "UNKNOWN";
//   case EIR_EXPR_UNARY_PLUS:
//     return "PLUS";
//   case EIR_EXPR_UNARY_MINUS:
//     return "MINUS";
//   case EIR_EXPR_UNARY_LOGICAL_NOT:
//     return "LOGICAL_NOT";
//   case EIR_EXPR_UNARY_BITWISE_NOT:
//     return "BITWISE_NOT";
//   case EIR_EXPR_UNARY_INC:
//     return "INC";
//   case EIR_EXPR_UNARY_DEC:
//     return "DEC";
//   case EIR_EXPR_UNARY_LOAD:
//     return "LOAD";
//   default:
//     return "UNKNOWN";
//   }
// }

// const char *eir_expr_binary_enum_str(eir_expr_binary_enum type) {
//   switch (type) {
//   case EIR_EXPR_BINARY_UNKNOWN:
//     return "UNKNOWN";
//   case EIR_EXPR_BINARY_STORE:
//     return "STORE";
//   case EIR_EXPR_BINARY_LOGICAL_OR:
//     return "LOGICAL_OR";
//   case EIR_EXPR_BINARY_LOGICAL_AND:
//     return "LOGICAL_AND";
//   case EIR_EXPR_BINARY_BITWISE_OR:
//     return "BITWISE_OR";
//   case EIR_EXPR_BINARY_BITWISE_XOR:
//     return "BITWISE_XOR";
//   case EIR_EXPR_BINARY_BITWISE_AND:
//     return "BITWISE_AND";
//   case EIR_EXPR_BINARY_EQUALS:
//     return "EQUALS";
//   case EIR_EXPR_BINARY_NOT_EQUALS:
//     return "NOT_EQUALS";
//   case EIR_EXPR_BINARY_LESS:
//     return "LESS";
//   case EIR_EXPR_BINARY_LESS_EQUALS:
//     return "LESS_EQUALS";
//   case EIR_EXPR_BINARY_GREATER:
//     return "GREATER";
//   case EIR_EXPR_BINARY_GREATER_EQUALS:
//     return "GREATER_EQUALS";
//   case EIR_EXPR_BINARY_BITWISE_SHIFT_LEFT:
//     return "BITWISE_SHIFT_LEFT";
//   case EIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT:
//     return "BITWISE_SHIFT_RIGHT";
//   case EIR_EXPR_BINARY_ADD:
//     return "ADD";
//   case EIR_EXPR_BINARY_SUB:
//     return "SUB";
//   case EIR_EXPR_BINARY_MUL:
//     return "MUL";
//   case EIR_EXPR_BINARY_DIV:
//     return "DIV";
//   case EIR_EXPR_BINARY_MOD:
//     return "MOD";
//   default:
//     return "UNKNOWN";
//   }
// }

// const char *eir_expr_builtin_enum_str(eir_expr_builtin_enum type) {
//   switch (type) {
//   case EIR_EXPR_BUILTIN_CAST:
//     return "CAST";
//   case EIR_EXPR_BUILTIN_MAKE:
//     return "MAKE";
//   default:
//     return "UNKNOWN";
//   }
// }
