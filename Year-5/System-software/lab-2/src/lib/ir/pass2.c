#include "pass2.h"
#include "exception.h"
#include "ir/ctx.h"
#include "ir/expr.h"
#include "ir/type.h"
#include "util/log.h"
#include "util/macro.h"
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static long ir_pass2_integer(const char *str, int base, char **err_str_out) {
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

void ir_pass2_init(ir_pass2 *self, struct ir_ctx *ctx, const char *file_name) {
  self->base.kind = IR_PASS_2;
  self->ctx = ctx;
  self->file_name = file_name;
}

ir_term_lit *ir_pass2_str(ir_pass2 *self, const char *str) {
  debug("pass2 creating str %s", str);
  UNUSED(self);

  ir_term_lit_v u;
  u.v_str = strdup(str);
  return ir_term_lit_new(IR_T_STRING, u);
}

ir_term_lit *ir_pass2_rune(ir_pass2 *self, const char *str) {
  debug("pass2 creating rune %s", str);
  // NOTE: checked by lexer
  if (strlen(str) != 3 && str[0] != '\'' && str[2] != '\'') {
    exception *ex = exception_new_f(EXCEPTION_IR, EXCEPTION_IR_CHAR_VALIDATION,
                                    self->file_name, 0, 0, "char validation",
                                    "char %s not valid", str);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }
  ir_term_lit_v u;
  u.v_char = str[1];
  return ir_term_lit_new(IR_T_CHAR, u);
}

ir_term_lit *ir_pass2_hex(ir_pass2 *self, const char *str) {
  UNUSED(self);
  debug("pass2 creating hex %s", str);

  ir_term_lit_v u;
  char         *errstr = NULL;
  u.v_int = ir_pass2_integer(str + 2, 16, &errstr);

  if (errstr) {
    exception *ex =
        exception_new_f(EXCEPTION_IR, EXCEPTION_IR_HEX_VALIDATION,
                        self->file_name, 0, 0, "hex validation", errstr);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 hex=%d", u.v_int);

  // TODO: calculate optimal type
  return ir_term_lit_new(IR_T_LONG, u);
}

ir_term_lit *ir_pass2_bits(ir_pass2 *self, const char *str) {
  UNUSED(self);
  debug("pass2 creating bits %s", str);

  ir_term_lit_v u;
  char         *errstr = NULL;
  u.v_int = ir_pass2_integer(str + 2, 2, &errstr);

  if (errstr) {
    exception *ex =
        exception_new_f(EXCEPTION_IR, EXCEPTION_IR_BITS_VALIDATION,
                        self->file_name, 0, 0, "bits validation", errstr);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 bits=%d", u.v_int);

  // TODO: calculate optimal type
  return ir_term_lit_new(IR_T_LONG, u);
}

ir_term_lit *ir_pass2_dec(ir_pass2 *self, const char *str) {
  UNUSED(self);
  debug("pass2 creating dec %s", str);

  ir_term_lit_v u;
  char         *errstr = NULL;
  u.v_int = ir_pass2_integer(str, 10, &errstr);

  if (errstr) {
    exception *ex =
        exception_new_f(EXCEPTION_IR, EXCEPTION_IR_DEC_VALIDATION,
                        self->file_name, 0, 0, "dec validation", errstr);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 dec=%d", u.v_int);

  // TODO: calculate optimal type
  return ir_term_lit_new(IR_T_LONG, u);
}

ir_term_lit *ir_pass2_bool(ir_pass2 *self, const char *str) {
  UNUSED(self);
  debug("pass2 creating bool %s", str);

  ir_term_lit_v u;

  if (!strcmp("true", str)) {
    u.v_bool = 1;
  } else if (!strcmp("false", str)) {
    u.v_bool = 0;
  } else {
    exception *ex = exception_new_f(EXCEPTION_IR, EXCEPTION_IR_BOOL_VALIDATION,
                                    self->file_name, 0, 0, "bool validation",
                                    "%s is invalid", str);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 bool=%d", u.v_bool);

  return ir_term_lit_new(IR_T_BOOL, u);
}

ir_term_id *ir_pass2_term_id_new(ir_pass2 *self, const char *id) {
  UNUSED(self);
  return ir_term_id_new(id);
}

// TODO: types
ir_expr_base *ir_pass2_expr_id_new(ir_pass2 *self, const ir_term_id *id) {
  UNUSED(self);

  return (ir_expr_base *)ir_expr_unary_new((ir_expr_base *)ir_expr_id_new(id),
                                           IR_EXPR_UNARY_LOAD);
}

// TODO: types
ir_expr_base *ir_pass2_expr_lit_new(ir_pass2 *self, ir_term_lit *lit) {
  UNUSED(self);
  return (ir_expr_base *)ir_expr_lit_new(lit);
}

// TODO: types
ir_expr_base *ir_pass2_expr_index_new(ir_pass2 *self, ir_expr_base *indexed,
                                      p_list args) {
  UNUSED(self);
  return (ir_expr_base *)ir_expr_index_new(indexed, args);
}

// TODO: types
ir_expr_base *ir_pass2_expr_call_new(ir_pass2 *self, ir_expr_base *callee,
                                     p_list args) {
  UNUSED(self);
  return (ir_expr_base *)ir_expr_call_new(callee, args);
}

// TODO: types
ir_expr_base *ir_pass2_expr_unary_new(ir_pass2 *self, ir_expr_base *first,
                                      ir_expr_unary_enum op) {
  debug("creating unary expression with op=%d", op);

  switch (op) {
  case IR_EXPR_UNARY_DEC:
  case IR_EXPR_UNARY_INC: {
    if (!first) {
      exception *ex =
          exception_new_f(EXCEPTION_IR, EXCEPTION_IR_NULL, self->file_name, 0,
                          0, "null expression", "unary expression is <null>");
      ir_ctx_add_exception(self->ctx, ex);
      goto error;
    }
    if (first->kind != IR_EXPR_IDENTIFIER) {
      exception *ex = exception_new_f(
          EXCEPTION_IR, EXCEPTION_IR_RVALUE_ASSIGNMENT, self->file_name, 0, 0,
          "rvalue assignment", "cannot assign to rvalue");
      ir_ctx_add_exception(self->ctx, ex);
      goto error;
    }

    ir_term_lit_v u;
    u.v_int = 1;

    ir_expr_base *second;
    if (op == IR_EXPR_UNARY_INC) {
      second = (ir_expr_base *)ir_expr_binary_new(
          first, (ir_expr_base *)ir_expr_lit_new(ir_term_lit_new(IR_T_LONG, u)),
          IR_EXPR_BINARY_ADD);
    } else {
      second = (ir_expr_base *)ir_expr_binary_new(
          first, (ir_expr_base *)ir_expr_lit_new(ir_term_lit_new(IR_T_LONG, u)),
          IR_EXPR_BINARY_SUB);
    }
    return (ir_expr_base *)ir_expr_binary_new(first, second,
                                              IR_EXPR_BINARY_ASSIGN);
  }
  case IR_EXPR_UNKNOWN:
    error("unknown unary expression type=%d", op);
    goto error;
  default:
    return (ir_expr_base *)ir_expr_unary_new(first, op);
  }

error:
  ir_expr_free(first);
  return NULL;
}

ir_expr_base *ir_pass2_expr_binary_new(ir_pass2 *self, ir_expr_base *first,
                                       ir_expr_base       *second,
                                       ir_expr_binary_enum op) {
  UNUSED(self);

  error("creating binary expression type=%d", op);

  switch (op) {
  case IR_EXPR_BINARY_UNKNOWN:
    error("unknown binary expression type=%d", op);
    goto error;
  default:
    return (ir_expr_base *)ir_expr_binary_new(first, second, op);
  }

error:
  ir_expr_free(first);
  ir_expr_free(second);
  return NULL;
}

// int status = ir_subroutine_status(lsv);

// if (status == IR_SUBROUTINE_STATUS_DEFINITION) {
//   // TODO: may replace 0-s with actual location
//   exception *ex = exception_new_f(
//       EXCEPTION_IR, EXCEPTION_IR_FUNCTION_REDEFINITION,
//       subroutine->file_name, 0, 0, "function redefinition",
//       "previous definition in %s %d:%d", lsv->file_name, 0, 0);
//   ir_ctx_add_exception(ctx, ex);
//   ret = -1;
//   break;
// }
