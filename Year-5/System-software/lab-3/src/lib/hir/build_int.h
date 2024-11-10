#pragma once

#include "hir/build.h"

typedef struct hir_id_struct {
  span       *span;
  const char *name;
} hir_id;

hir_id hir_build_id(hir_ctx *ctx, ANTLR3_BASE_TREE *node);

type_base   *hir_build_type_bool(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_byte(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_int(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_uint(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_long(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_ulong(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_char(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_base   *hir_build_type_string(hir_ctx *ctx, ANTLR3_BASE_TREE *node);
type_custom *hir_build_type_custom(hir_ctx *ctx, hir_id id);
type_array  *hir_build_type_array(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                  ANTLR3_BASE_TREE *dimensions,
                                  type_base        *elem_type);

void hir_build_func_pre(hir_ctx *ctx);
void hir_buid_func(hir_ctx *ctx);
void hir_build_func_signature(hir_ctx *ctx);
void hir_build_arg_list_pre(hir_ctx *ctx);
void hir_build_arg_list(hir_ctx *ctx);
void hir_build_arg(hir_ctx *ctx);
void hir_build_type(hir_ctx *ctx);
