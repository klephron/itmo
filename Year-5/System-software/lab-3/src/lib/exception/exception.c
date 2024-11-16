#include "exception.h"

#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "util/colors.h"
#include "util/macro.h"
#include "util/string_buffer.h"

void exception_free(exception *exc) {
  if (exc) {
    if (exc->stream) {
      free(exc->stream);
    }

    if (exc->name) {
      free(exc->name);
    }

    if (exc->message) {
      free(exc->message);
    }

    free(exc);
  }
}

exception *exception_new(exception_level level, exception_type type,
                         int subtype, const char *stream, uint32_t line,
                         uint32_t offset, const char *name,
                         const char *message) {

  exception *exc = calloc(1, sizeof(exception));

  exc->level = level;
  exc->type = type;
  exc->subtype = subtype;

  if (stream) {
    exc->stream = strdup(stream);
  }
  exc->line = line;
  exc->offset = offset;

  if (name) {
    exc->name = strdup(name);
  }

  if (message) {
    exc->message = strdup(message);
  }

  return exc;
};

exception *exception_new_v(exception_level level, exception_type type,
                           int subtype, const char *stream, uint32_t line,
                           uint32_t offset, const char *name,
                           const char *format, va_list args) {
  char message[256]; // message size
  message[0] = '\0';
  vsnprintf(message, 255, format, args);
  return exception_new(level, type, subtype, stream, line, offset, name,
                       message);
}

exception *exception_new_f(exception_level level, exception_type type,
                           int subtype, const char *stream, uint32_t line,
                           uint32_t offset, const char *name,
                           const char *format, ...) {
  exception *exc;
  va_list    args;
  va_start(args, format);
  exc = exception_new_v(level, type, subtype, stream, line, offset, name,
                        format, args);
  va_end(args);
  return exc;
}

char *exception_str(const exception *exc) {
  char buf[256];

  char *exc_stream = "<empty>";
  char *exc_name = "<empty>";
  char *exc_message = "<empty>";

  {
    if (exc->stream) {
      exc_stream = exc->stream;
    }

    if (exc->name) {
      exc_name = exc->name;
    }

    if (exc->message) {
      exc_message = exc->message;
    }
  }

  string_buffer *buffer = string_buffer_new(sizeof(buf), 0);

  switch (exc->level) {
  case EXCEPTION_LEVEL_INFO:
    string_buffer_append(buffer, ANSI_COLOR_CYAN "info" ANSI_COLOR_RESET);
    break;
  case EXCEPTION_LEVEL_WARNING:
    string_buffer_append(buffer, ANSI_COLOR_YELLOW "warning" ANSI_COLOR_RESET);
    break;
  case EXCEPTION_LEVEL_ERROR:
    string_buffer_append(buffer, ANSI_COLOR_RED "error" ANSI_COLOR_RESET);
    break;
  default:
    string_buffer_append(buffer, "unknown");
    break;
  }

  switch (exc->type) {
  case EXCEPTION_LEXER:
    snprintf(buf, STRMAXLEN(buf), "(L%d): %s\n", exc->subtype, exc_message);
    break;
  case EXCEPTION_PARSER:
    snprintf(buf, STRMAXLEN(buf), "(P%d): %s -> %s", exc->subtype, exc_name,
             exc_message);
    break;
  case EXCEPTION_TREE:
    snprintf(buf, STRMAXLEN(buf), "(T%d): %s -> %s", exc->subtype, exc_name,
             exc_message);
    break;
  case EXCEPTION_HIR:
    snprintf(buf, STRMAXLEN(buf), "(H%d): %s -> %s", exc->subtype, exc_name,
             exc_message);
    break;
  case EXCEPTION_EIR:
    snprintf(buf, STRMAXLEN(buf), "(E%d): %s -> %s", exc->subtype, exc_name,
             exc_message);
    break;
  default:
    snprintf(buf, STRMAXLEN(buf), "(U%d): %s -> %s", exc->subtype, exc_name,
             exc_message);
    break;
  }
  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, "\n");

  snprintf(buf, STRMAXLEN(buf),
           ANSI_COLOR_BLUE "  -->" ANSI_COLOR_RESET " %s:%d:%d", exc_stream,
           exc->line, exc->offset);
  string_buffer_append(buffer, buf);

  return string_buffer_detach(buffer);
}

exception *exception_new_from_helper(const exception_helper *helper,
                                     char                   *format, ...) {
  va_list args;
  va_start(args, format);
  exception *exc = exception_new_v(helper->level, helper->type, helper->subtype,
                                   helper->stream, helper->line, helper->offset,
                                   helper->name, format, args);
  va_end(args);
  return exc;
}

const char *exception_subtype_hir_str(exception_subtype_hir type) {
  switch (type) {
  case EXCEPTION_HIR_HEX_VALIDATION:
    return "hex validation";
  case EXCEPTION_HIR_BITS_VALIDATION:
    return "bits validation";
  case EXCEPTION_HIR_DEC_VALIDATION:
    return "dec validation";
  case EXCEPTION_HIR_BOOL_VALIDATION:
    return "bool validation";
  case EXCEPTION_HIR_SYMBOL_REDEFINITION:
    return "symbol redefinition";
  case EXCEPTION_HIR_SYMBOL_UNDEFINED:
    return "symbol undefined";
  case EXCEPTION_HIR_SUBROUTINE_DECL_INVALID:
    return "invalid subroutine declaration";
  case EXCEPTION_HIR_TYPE_UNSUPPORTED:
    return "type unsupported";
  case EXCEPTION_HIR_UNKNOWN:
  default:
    return "unknown";
  }
}

const char *exception_subtype_eir_str(exception_subtype_eir type) {
  switch (type) {
  case EXCEPTION_EIR_NULL:
    return "null";
  case EXCEPTION_EIR_RVALUE_ASSIGNMENT:
    return "rvalue assignment";
  default:
    return "unknown";
  }
}
