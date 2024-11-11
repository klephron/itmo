#pragma once

#include "hir/node.h"

typedef enum hir_stmt_enum {
  HIR_STMT_UNKNOWN,
  HIR_STMT_IF,
  HIR_STMT_BLOCK,
  HIR_STMT_WHILE,
  HIR_STMT_DO,
  HIR_STMT_BREAK,
  HIR_STMT_EXPR,
} hir_stmt_enum;

typedef struct hir_stmt_base_struct {
  hir_base      base;
  hir_stmt_enum kind;
} hir_stmt_base;

void hir_stmt_free(hir_stmt_base *generic);
