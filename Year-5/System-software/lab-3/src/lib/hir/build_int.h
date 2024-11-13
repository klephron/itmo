#pragma once

#include "hir/build.h"

hir_id *hir_build_id(hir_ctx *ctx, ANTLR3_BASE_TREE *node);

hir_lit *hir_build_str(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
hir_lit *hir_build_rune(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
hir_lit *hir_build_hex(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
hir_lit *hir_build_bits(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
hir_lit *hir_build_dec(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
hir_lit *hir_build_bool(hir_ctx *ctx, ANTLR3_BASE_TREE *node);

type_base   *hir_build_type_bool(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_byte(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_int(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_uint(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_long(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_ulong(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_char(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_string(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_custom *hir_build_type_custom(hir_ctx *ctx, hir_id *id);
type_array  *hir_build_type_array(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                  ANTLR3_BASE_TREE *dimensions,
                                  type_base        *elem_type);

hir_param      *hir_build_param(hir_ctx *ctx, hir_id *id, type_base *type);
hir_subroutine *hir_build_signature(hir_ctx *ctx, ANTLR3_BASE_TREE *root,
                                    hir_id *id, list_hir_param *params,
                                    type_base *ret_type);
list_hir_var   *hir_build_body_var(hir_ctx *ctx, list_hir_id *ids,
                                   type_base *type);
void            hir_build_body_var_extend(hir_ctx *ctx, list_hir_var *first,
                                          list_hir_var *second);
void            hir_build_func(hir_ctx *ctx, hir_subroutine *subroutine,
                               list_hir_var *vars, hir_stmt_block *body);

hir_expr_lit    *hir_build_expr_lit(hir_ctx *ctx, hir_lit *lit);
hir_expr_id     *hir_build_expr_id(hir_ctx *ctx, hir_id *id);
hir_expr_index  *hir_build_expr_indexer(hir_ctx *ctx, hir_expr_base *indexed,
                                        list_hir_expr *args);
hir_expr_call   *hir_build_expr_caller(hir_ctx *ctx, hir_expr_base *indexed,
                                       list_hir_expr *args);
hir_expr_unary  *hir_build_expr_unary(hir_ctx *ctx, hir_expr_base *first,
                                      ANTLR3_BASE_TREE   *op_node,
                                      hir_expr_unary_enum op);
hir_expr_binary *hir_build_expr_binary(hir_ctx *ctx, hir_expr_base *first,
                                       hir_expr_base       *second,
                                       ANTLR3_BASE_TREE    *op_node,
                                       hir_expr_binary_enum op);

hir_stmt_if    *hir_build_stmt_if(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                  hir_expr_base *cond, hir_stmt_base *je,
                                  hir_stmt_base *jz);
hir_stmt_block *hir_build_stmt_block(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                     list_hir_stmt *stmts);
hir_stmt_while *hir_build_stmt_while(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                     hir_expr_base *cond, hir_stmt_base *stmt);
hir_stmt_do    *hir_build_stmt_do(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                  int positive, hir_expr_base *cond,
                                  hir_stmt_base *stmt);
hir_stmt_break *hir_build_stmt_break(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
hir_stmt_expr  *hir_build_stmt_expr(hir_ctx *ctx, hir_expr_base *expr);
