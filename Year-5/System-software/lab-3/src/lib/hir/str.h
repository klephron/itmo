#pragma once

#include "hir/node_expr.h"

const char *hir_expr_unary_enum_str(hir_expr_unary_enum type);
const char *hir_expr_binary_enum_str(hir_expr_binary_enum type);

const char *hir_binding_enum_str(hir_binding_enum kind);

char *hir_list_symbol_str(const list_hir_symbol *symbols);
