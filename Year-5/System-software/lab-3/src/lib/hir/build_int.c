#include "build_int.h"
#include "antlr3/antlr3.h"
#include "errno.h"
#include "hir/symbols.h"
#include "util/log.h"
#include "util/macro.h"
#include <antlr3commontoken.h>

static span *hir_span_new(ANTLR3_BASE_TREE *first, const char *source) {
  if (!first || !first->getToken(first)) {
    debug("span is set to default");
    return span_new(source, 0, 0, 0, 0);
  }
  ANTLR3_COMMON_TOKEN *first_token = first->getToken(first);

  span *span = span_new(source, first_token->line, first_token->line,
                        first_token->getCharPositionInLine(first_token),
                        first_token->getCharPositionInLine(first_token) +
                            (first_token->stop - first_token->start + 1));
  return span;
}

static span *hir_span_new_range(const span *first, const span *second) {
  span *span = span_new(first->source, first->line_start, second->line_end,
                        first->pos_start, second->pos_end);
  return span;
}

static long hir_build_str2long(const char *str, int base, char **err_str_out) {
  char errbuf[256];
  errbuf[0] = '\0';

  errno = 0;
  char *endptr = NULL;
  long  number = strtol(str, &endptr, base);

  /* test return to number and errno values */
  if (str == endptr)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (no digits found, 0 returned)\n", number);
  else if (errno == ERANGE && number == LONG_MIN)
    snprintf(errbuf, sizeof(errbuf) - 1, "%lu invalid (underflow occurred)",
             number);
  else if (errno == ERANGE && number == LONG_MAX)
    snprintf(errbuf, sizeof(errbuf) - 1, "%lu invalid (overflow occurred)",
             number);
  else if (errno == EINVAL) /* not in all c99 implementations - gcc OK */
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (base contains unsupported value)", number);
  else if (errno != 0 && number == 0)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (unspecified error occurred)", number);
  else if (errno == 0 && str && *endptr != 0)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu valid (but additional characters remain)", number);

  if (errbuf[0] != '\0') {
    *err_str_out = malloc(sizeof(errbuf));
    memcpy(*err_str_out, errbuf, sizeof(errbuf));
  }

  return number;
}

static ulong hir_build_str2ulong(const char *str, int base,
                                 char **err_str_out) {
  char errbuf[256];
  errbuf[0] = '\0';

  errno = 0;
  char *endptr = NULL;
  ulong number = strtoul(str, &endptr, base);

  /* test return to number and errno values */
  if (str == endptr)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (no digits found, 0 returned)\n", number);
  else if (errno == ERANGE && number == 0)
    snprintf(errbuf, sizeof(errbuf) - 1, "%lu invalid (underflow occurred)",
             number);
  else if (errno == ERANGE && number == ULONG_MAX)
    snprintf(errbuf, sizeof(errbuf) - 1, "%lu invalid (overflow occurred)",
             number);
  else if (errno == EINVAL) /* not in all c99 implementations - gcc OK */
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (base contains unsupported value)", number);
  else if (errno != 0 && number == 0)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (unspecified error occurred)", number);
  else if (errno == 0 && str && *endptr != 0)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu valid (but additional characters remain)", number);

  if (errbuf[0] != '\0') {
    *err_str_out = malloc(sizeof(errbuf));
    memcpy(*err_str_out, errbuf, sizeof(errbuf));
  }

  return number;
}

static void hir_exception_add(hir_ctx *ctx, exception_subtype_ir subtype,
                              const span *span, const char *format, ...) {
  exception *exc;
  va_list    args;
  va_start(args, format);

  exc = exception_new_v(EXCEPTION_IR, subtype, span->source, span->line_start,
                        span->pos_start, exception_subtype_ir_str(subtype),
                        format, args);
  list_exception_push_back(ctx->exceptions, exc);

  va_end(args);
}

// IDENTIFIER
hir_id *hir_build_id(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  const char *name = ANTLR3_CHARS(node);
  return hir_id_new(hir_span_new(node, ctx->stream), strdup(name));
}

// LITERALS
hir_lit *hir_build_str(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span       *span = hir_span_new(node, ctx->stream);
  const char *chars = ANTLR3_CHARS(node);
  return hir_lit_new(span, type_base_new(TYPE_STRING, span_copy(span)),
                     (hir_lit_u){.v_str = strdup(chars)});
}

hir_lit *hir_build_rune(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span       *span = hir_span_new(node, ctx->stream);
  const char *chars = ANTLR3_CHARS(node);
  return hir_lit_new(span, type_base_new(TYPE_CHAR, span_copy(span)),
                     (hir_lit_u){.v_char = strdup(chars)});
}

hir_lit *hir_build_hex(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span       *span = hir_span_new(node, ctx->stream);
  const char *chars = ANTLR3_CHARS(node) + 2;

  char *err_msg = NULL;
  long  value = hir_build_str2long(chars, 16, &err_msg);
  if (err_msg) {
    free(err_msg);
    err_msg = NULL;
    ulong uvalue = hir_build_str2ulong(chars, 16, &err_msg);
    if (err_msg) {
      hir_exception_add(ctx, EXCEPTION_IR_HEX_VALIDATION, span, err_msg);
      free(err_msg);
      return NULL;
    }
    return hir_lit_new(span, type_base_new(TYPE_ULONG, NULL),
                       (hir_lit_u){.v_ulong = uvalue});
  }
  return hir_lit_new(span, type_base_new(TYPE_LONG, NULL),
                     (hir_lit_u){.v_long = value});
}

hir_lit *hir_build_bits(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span       *span = hir_span_new(node, ctx->stream);
  const char *chars = ANTLR3_CHARS(node) + 2;

  char *err_msg = NULL;
  long  value = hir_build_str2long(chars, 2, &err_msg);
  if (err_msg) {
    free(err_msg);
    err_msg = NULL;
    ulong uvalue = hir_build_str2ulong(chars, 2, &err_msg);
    if (err_msg) {
      hir_exception_add(ctx, EXCEPTION_IR_BITS_VALIDATION, span, err_msg);
      free(err_msg);
      return NULL;
    }
    return hir_lit_new(span, type_base_new(TYPE_ULONG, NULL),
                       (hir_lit_u){.v_ulong = uvalue});
  }
  return hir_lit_new(span, type_base_new(TYPE_LONG, NULL),
                     (hir_lit_u){.v_long = value});
}

hir_lit *hir_build_dec(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span       *span = hir_span_new(node, ctx->stream);
  const char *chars = ANTLR3_CHARS(node);

  char *err_msg = NULL;
  long  value = hir_build_str2long(chars, 10, &err_msg);
  if (err_msg) {
    free(err_msg);
    err_msg = NULL;
    ulong uvalue = hir_build_str2ulong(chars, 10, &err_msg);
    if (err_msg) {
      hir_exception_add(ctx, EXCEPTION_IR_DEC_VALIDATION, span, err_msg);
      free(err_msg);
      return NULL;
    }
    return hir_lit_new(span, type_base_new(TYPE_ULONG, NULL),
                       (hir_lit_u){.v_ulong = uvalue});
  }
  return hir_lit_new(span, type_base_new(TYPE_LONG, NULL),
                     (hir_lit_u){.v_long = value});
}

hir_lit *hir_build_bool(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span       *span = hir_span_new(node, ctx->stream);
  const char *chars = ANTLR3_CHARS(node);

  hir_lit_u u;
  if (!strcmp("true", chars)) {
    u.v_bool = 1;
  } else if (!strcmp("false", chars)) {
    u.v_bool = 0;
  } else {
    hir_exception_add(ctx, EXCEPTION_IR_BOOL_VALIDATION, span,
                      "%s is invalid boolean const", chars);
    return NULL;
  }
  return hir_lit_new(span, type_base_new(TYPE_BOOL, NULL), u);
}

// TYPES
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

type_custom *hir_build_type_custom(hir_ctx *ctx, hir_id *id) {
  UNUSED(ctx);
  hir_id_data  id_data = hir_id_pop(id);
  type_custom *custom = type_custom_new(id_data.span, id_data.name);
  return custom;
}

type_array *hir_build_type_array(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                 ANTLR3_BASE_TREE *dimensions_node,
                                 type_base        *elem_type) {
  span  *root = hir_span_new(node, ctx->stream);
  size_t dimensions = ANTLR3_SIZE(dimensions_node) + 1;
  span  *span_m = hir_span_new_range(root, elem_type->span);
  span_free(root);
  return type_array_new(span_m, dimensions, elem_type);
}

hir_param *hir_build_param(hir_ctx *ctx, hir_id *id, type_base *type) {
  UNUSED(ctx);
  span *span;
  if (type) {
    span = hir_span_new_range(id->base.span, type->span);
  } else {
    span = span_copy(id->base.span);
  }
  return hir_param_new(span, id, type);
}

hir_subroutine *hir_build_signature(hir_ctx *ctx, ANTLR3_BASE_TREE *root,
                                    hir_id *id, list_hir_param *params,
                                    type_base *ret_type) {
  span *root_span = hir_span_new(root, ctx->stream);
  span *span;
  if (ret_type) {
    span = hir_span_new_range(root_span, ret_type->span);
  } else if (list_hir_param_size(params) > 0) {
    span =
        hir_span_new_range(root_span, list_hir_param_back(params)->base.span);
  } else {
    span = hir_span_new_range(root_span, id->base.span);
  }
  span_free(root_span);
  // debug("%zu:%zu %zu:%zu", span->line_start, span->pos_start, span->line_end,
  // span->pos_end);
  return hir_subroutine_new(span, id, params, ret_type, NULL, NULL, NULL);
}

list_hir_var *hir_build_body_var(hir_ctx *ctx, list_hir_id *ids,
                                 type_base *type) {
  UNUSED(ctx);
  list_hir_var *var_list = list_hir_var_new();

  size_t id_size = list_hir_id_size(ids);
  for (size_t i = 1; i < id_size; ++i) {
    hir_id *id = list_hir_id_pop_front(ids);
    list_hir_var_push_back(
        var_list, hir_var_new(span_copy(id->base.span), id, type_copy(type)));
  }
  if (id_size > 0) {
    hir_id *id = list_hir_id_pop_front(ids);
    list_hir_var_push_back(var_list,
                           hir_var_new(span_copy(id->base.span), id, type));
  }

  list_hir_id_free(ids);
  return var_list;
}

void hir_build_body_var_extend(hir_ctx *ctx, list_hir_var *first,
                               list_hir_var *second) {
  UNUSED(ctx);
  size_t size = list_hir_var_size(second);
  for (size_t i = 0; i < size; ++i) {
    hir_var *var = list_hir_var_pop_front(second);
    list_hir_var_push_back(first, var);
  }
  list_hir_var_free(second);
}

void hir_build_func(hir_ctx *ctx, hir_subroutine *subroutine,
                    list_hir_var *vars, hir_stmt_block *body) {
  subroutine->vars = vars;
  subroutine->body = body;

  type_list *types = type_list_new();
  for (list_hir_param_it it = list_hir_param_begin(subroutine->params);
       !list_hir_param_it_end(&it); list_hir_param_it_next(&it)) {
    type_base *type = list_hir_param_it_get(&it)->type;
    type_list_push_back(types, type_copy(type));
  }

  list_hir_symbol_push_back(
      ctx->hir->globals,
      hir_symbol_new(strdup(subroutine->id->name), HIR_BINDING_GLOBAL, NULL,
                     (type_base *)type_callable_new(
                         NULL, type_copy(subroutine->ret_type), types),
                     span_copy(subroutine->base.span)));

  list_hir_subroutine_push_back(ctx->hir->subroutines, subroutine);
}

// EXPRESSIONS
hir_expr_lit *hir_build_expr_lit(hir_ctx *ctx, hir_lit *lit) {
  UNUSED(ctx);
  return hir_expr_lit_new(span_copy(lit->base.span), NULL, lit);
}

hir_expr_id *hir_build_expr_id(hir_ctx *ctx, hir_id *id) {
  UNUSED(ctx);
  return hir_expr_id_new(span_copy(id->base.span), NULL, id, NULL);
}

hir_expr_index *hir_build_expr_indexer(hir_ctx *ctx, hir_expr_base *indexed,
                                       list_hir_expr *args) {
  UNUSED(ctx);
  span *span;
  if (list_hir_expr_size(args) > 0) {
    span = hir_span_new_range(indexed->base.span,
                              list_hir_expr_back(args)->base.span);
  } else {
    span = span_copy(indexed->base.span);
  }
  return hir_expr_index_new(span, NULL, indexed, args);
}

hir_expr_call *hir_build_expr_caller(hir_ctx *ctx, hir_expr_base *indexed,
                                     list_hir_expr *args) {
  UNUSED(ctx);
  span *span;
  if (list_hir_expr_size(args) > 0) {
    span = hir_span_new_range(indexed->base.span,
                              list_hir_expr_back(args)->base.span);
  } else {
    span = span_copy(indexed->base.span);
  }
  return hir_expr_call_new(span, NULL, indexed, args);
}

hir_expr_unary *hir_build_expr_unary(hir_ctx *ctx, hir_expr_base *first,
                                     ANTLR3_BASE_TREE   *op_node,
                                     hir_expr_unary_enum op) {
  span *op_span = hir_span_new(op_node, ctx->stream);
  span *span;
  if (op_span->line_start < first->base.span->line_start) {
    span = hir_span_new_range(op_span, first->base.span);
  } else {
    span = hir_span_new_range(first->base.span, op_span);
  }
  span_free(op_span);
  return hir_expr_unary_new(span, NULL, op, first);
}

hir_expr_binary *hir_build_expr_binary(hir_ctx *ctx, hir_expr_base *first,
                                       hir_expr_base       *second,
                                       ANTLR3_BASE_TREE    *op_node,
                                       hir_expr_binary_enum op) {
  UNUSED(ctx);
  UNUSED(op_node);
  return hir_expr_binary_new(
      hir_span_new_range(first->base.span, second->base.span), NULL, op, first,
      second);
}

// STATEMENTS
hir_stmt_if *hir_build_stmt_if(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                               hir_expr_base *cond, hir_stmt_base *je,
                               hir_stmt_base *jz) {
  span *node_span = hir_span_new(node, ctx->stream);
  span *span;
  if (jz) {
    span = hir_span_new_range(node_span, jz->base.span);
  } else {
    span = hir_span_new_range(node_span, je->base.span);
  }
  span_free(node_span);
  return hir_stmt_if_new(span, cond, je, jz);
}

hir_stmt_block *hir_build_stmt_block(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                     list_hir_stmt *stmts) {
  span *node_span = hir_span_new(node, ctx->stream);
  span *span;
  if (list_hir_stmt_size(stmts) > 0) {
    span = hir_span_new_range(node_span, list_hir_stmt_back(stmts)->base.span);
    span_free(node_span);
  } else {
    span = node_span;
  }
  return hir_stmt_block_new(span, stmts);
}

hir_stmt_while *hir_build_stmt_while(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                                     hir_expr_base *cond, hir_stmt_base *stmt) {
  span *node_span = hir_span_new(node, ctx->stream);
  span *span = hir_span_new_range(node_span, stmt->base.span);
  span_free(node_span);
  return hir_stmt_while_new(span, cond, stmt);
}

hir_stmt_do *hir_build_stmt_do(hir_ctx *ctx, ANTLR3_BASE_TREE *node,
                               int positive, hir_expr_base *cond,
                               hir_stmt_base *stmt) {
  span *node_span = hir_span_new(node, ctx->stream);
  span *span = hir_span_new_range(node_span, cond->base.span);
  span_free(node_span);
  return hir_stmt_do_new(span, positive, cond, stmt);
}

hir_stmt_break *hir_build_stmt_break(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  span *node_span = hir_span_new(node, ctx->stream);
  return hir_stmt_break_new(node_span);
}

hir_stmt_expr *hir_build_stmt_expr(hir_ctx *ctx, hir_expr_base *expr) {
  UNUSED(ctx);
  return hir_stmt_expr_new(span_copy(expr->base.span), expr);
}
