#include "pass2.h"
#include "exception.h"
#include "ir/ctx.h"
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
