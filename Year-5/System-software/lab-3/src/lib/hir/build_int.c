#include "build_int.h"
#include "antlr3/antlr3.h"
#include "util/log.h"
#include "util/macro.h"
#include <antlr3commontoken.h>

static span *hir_span_new(ANTLR3_BASE_TREE *first, const char *source) {
  if (!first || !first->getToken(first)) {
    debug("span is set to default");
    return span_new(source, 0, 0, 0, 0);
  }
  ANTLR3_COMMON_TOKEN *first_token = first->getToken(first);

  return span_new(source, first_token->line, first_token->line,
                  first_token->getCharPositionInLine(first_token),
                  first_token->getCharPositionInLine(first_token) +
                      (first_token->stop - first_token->start + 1));
}

static span *hir_span_new_range(span *first, span *second) {
  return span_new(first->source, first->line_start, second->line_end,
                  first->pos_start, second->pos_end);
}

hir_id hir_build_id(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  const char *name = ANTLR3_CHARS(node);
  return (hir_id){.name = name, .span = hir_span_new(node, ctx->stream)};
}

type_base *hir_build_type_bool(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_BOOL, span);
}

type_base *hir_build_type_byte(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_BYTE, span);
}

type_base *hir_build_type_int(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_INT, span);
}

type_base *hir_build_type_uint(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_UINT, span);
}

type_base *hir_build_type_long(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_LONG, span);
}

type_base *hir_build_type_ulong(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_ULONG, span);
}

type_base *hir_build_type_char(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_CHAR, span);
}

type_base *hir_build_type_string(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *span = hir_span_new(node, ctx->stream);
  return type_base_new(TYPE_STRING, span);
}

type_custom *hir_build_type_custom(hir_ctx *ctx, hir_id id) {
  UNUSED(ctx);
  return type_custom_new(id.span, id.name);
}

type_array *hir_build_type_array(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                 ANTLR3_BASE_TREE *dimensions_node,
                                 type_base        *elem_type) {
  span  *root = hir_span_new(node, ctx->stream);
  size_t dimensions = ANTLR3_SIZE(dimensions_node) + 1;
  span  *span = hir_span_new_range(root, elem_type->span);
  debug("stream=%s %d:%d %d:%d", span->source, span->line_start,
        span->pos_start, span->line_end, span->pos_end);
  span_free(root);
  return type_array_new(span, dimensions, elem_type);
}
