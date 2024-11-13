#include "str.h"

const char *hir_expr_unary_enum_str(hir_expr_unary_enum type) {
  switch (type) {
  case HIR_EXPR_UNARY_PLUS:
    return "PLUS";
  case HIR_EXPR_UNARY_MINUS:
    return "MINUS";
  case HIR_EXPR_UNARY_LOGICAL_NOT:
    return "LOGICAL_NOT";
  case HIR_EXPR_UNARY_BITWISE_NOT:
    return "BITWISE_NOT";
  case HIR_EXPR_UNARY_INC:
    return "INC";
  case HIR_EXPR_UNARY_DEC:
    return "DEC";
  default:
    return "UNKNOWN";
  }
}

const char *hir_expr_binary_enum_str(hir_expr_binary_enum type) {
  switch (type) {
  case HIR_EXPR_BINARY_ASSIGN:
    return "ASSIGN";
  case HIR_EXPR_BINARY_LOGICAL_OR:
    return "LOGICAL_OR";
  case HIR_EXPR_BINARY_LOGICAL_AND:
    return "LOGICAL_AND";
  case HIR_EXPR_BINARY_BITWISE_OR:
    return "BITWISE_OR";
  case HIR_EXPR_BINARY_BITWISE_XOR:
    return "BITWISE_XOR";
  case HIR_EXPR_BINARY_BITWISE_AND:
    return "BITWISE_AND";
  case HIR_EXPR_BINARY_EQUALS:
    return "EQUALS";
  case HIR_EXPR_BINARY_NOT_EQUALS:
    return "NOT_EQUALS";
  case HIR_EXPR_BINARY_LESS:
    return "LESS";
  case HIR_EXPR_BINARY_LESS_EQUALS:
    return "LESS_EQUALS";
  case HIR_EXPR_BINARY_GREATER:
    return "GREATER";
  case HIR_EXPR_BINARY_GREATER_EQUALS:
    return "GREATER_EQUALS";
  case HIR_EXPR_BINARY_BITWISE_SHIFT_LEFT:
    return "BITWISE_SHIFT_LEFT";
  case HIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT:
    return "BITWISE_SHIFT_RIGHT";
  case HIR_EXPR_BINARY_ADD:
    return "ADD";
  case HIR_EXPR_BINARY_SUB:
    return "SUB";
  case HIR_EXPR_BINARY_MUL:
    return "MUL";
  case HIR_EXPR_BINARY_DIV:
    return "DIV";
  case HIR_EXPR_BINARY_MOD:
    return "MOD";
  default:
    return "UNKNOWN";
  }
}
