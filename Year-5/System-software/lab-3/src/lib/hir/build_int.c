#include "build_int.h"
#include "antlr3/antlr3.h"
#include "errno.h"
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

static span *hir_span_new_range(const span *first, const span *second) {
  return span_new(first->source, first->line_start, second->line_end,
                  first->pos_start, second->pos_end);
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
hir_id hir_build_id(hir_ctx *ctx, ANTLR3_BASE_TREE *node) {
  const char *name = ANTLR3_CHARS(node);
  return (hir_id){.name = name, .span = hir_span_new(node, ctx->stream)};
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
  span_free(root);
  return type_array_new(span, dimensions, elem_type);
}
