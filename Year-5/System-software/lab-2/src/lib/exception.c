#include "exception.h"

#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "util/colors.h"
#include "util/string_buffer.h"

void exception_free(struct exception *exc) {
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

struct exception *exception_new(enum exception_type type, int subtype,
                                const char *stream, uint32_t line,
                                uint32_t offset, const char *name,
                                const char *message) {

  struct exception *exc = calloc(1, sizeof(struct exception));

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

struct exception *exception_new_v(enum exception_type type, int subtype,
                                  const char *stream, uint32_t line,
                                  uint32_t offset, const char *name,
                                  const char *format, va_list args) {
  char message[256]; // message size
  message[0] = '\0';
  vsnprintf(message, 255, format, args);
  return exception_new(type, subtype, stream, line, offset, name, message);
}

struct exception *exception_new_f(enum exception_type type, int subtype,
                                  const char *stream, uint32_t line,
                                  uint32_t offset, const char *name,
                                  const char *format, ...) {
  struct exception *exc;
  va_list           args;
  va_start(args, format);
  exc =
      exception_new_v(type, subtype, stream, line, offset, name, format, args);
  va_end(args);
  return exc;
}

static char *exception_unknown_to_chars(const struct exception *exc) {
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
  string_buffer_append(buffer, ANSI_COLOR_RED "error" ANSI_COLOR_RESET);

  snprintf(buf, sizeof(buf) - 1, "(U%d): %s -> %s\n", exc->subtype, exc_name,
           exc_message);
  string_buffer_append(buffer, buf);

  snprintf(buf, sizeof(buf) - 1,
           ANSI_COLOR_BLUE " -->" ANSI_COLOR_RESET " %s:%d:%d", exc_stream,
           exc->line, exc->offset);
  string_buffer_append(buffer, buf);

  return string_buffer_detach(buffer);
}

static char *exception_lexer_to_chars(const struct exception *exc) {
  char buf[256];

  char *exc_stream = "<empty>";
  char *exc_message = "<empty>";

  {
    if (exc->stream) {
      exc_stream = exc->stream;
    }

    if (exc->message) {
      exc_message = exc->message;
    }
  }

  string_buffer *buffer = string_buffer_new(sizeof(buf), 0);
  string_buffer_append(buffer, ANSI_COLOR_RED "error" ANSI_COLOR_RESET);

  snprintf(buf, sizeof(buf) - 1, "(L%d): %s\n", exc->subtype, exc_message);
  string_buffer_append(buffer, buf);

  snprintf(buf, sizeof(buf) - 1,
           ANSI_COLOR_BLUE " -->" ANSI_COLOR_RESET " %s:%d:%d", exc_stream,
           exc->line, exc->offset);
  string_buffer_append(buffer, buf);

  return string_buffer_detach(buffer);
}

static char *exception_parser_to_chars(const struct exception *exc) {
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
  string_buffer_append(buffer, ANSI_COLOR_RED "error" ANSI_COLOR_RESET);

  snprintf(buf, sizeof(buf) - 1, "(P%d): %s -> %s\n", exc->subtype, exc_name,
           exc_message);
  string_buffer_append(buffer, buf);

  snprintf(buf, sizeof(buf) - 1,
           ANSI_COLOR_BLUE " -->" ANSI_COLOR_RESET " %s:%d:%d", exc_stream,
           exc->line, exc->offset);
  string_buffer_append(buffer, buf);

  return string_buffer_detach(buffer);
}

static char *exception_tree_to_chars(const struct exception *exc) {
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
  string_buffer_append(buffer, ANSI_COLOR_RED "error" ANSI_COLOR_RESET);

  snprintf(buf, sizeof(buf) - 1, "(T%d): %s -> %s\n", exc->subtype, exc_name,
           exc_message);
  string_buffer_append(buffer, buf);

  snprintf(buf, sizeof(buf) - 1,
           ANSI_COLOR_BLUE " -->" ANSI_COLOR_RESET " %s:%d:%d", exc_stream,
           exc->line, exc->offset);
  string_buffer_append(buffer, buf);

  return string_buffer_detach(buffer);
}

static char *exception_ir_to_chars(const struct exception *exc) {
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
  string_buffer_append(buffer, ANSI_COLOR_RED "error" ANSI_COLOR_RESET);

  snprintf(buf, sizeof(buf) - 1, "(I%d): %s -> %s\n", exc->subtype, exc_name,
           exc_message);
  string_buffer_append(buffer, buf);

  snprintf(buf, sizeof(buf) - 1,
           ANSI_COLOR_BLUE " -->" ANSI_COLOR_RESET " %s:%d:%d", exc_stream,
           exc->line, exc->offset);
  string_buffer_append(buffer, buf);

  return string_buffer_detach(buffer);
}

char *exception_to_chars(const struct exception *exc) {
  switch (exc->type) {
  case EXCEPTION_LEXER:
    return exception_lexer_to_chars(exc);
  case EXCEPTION_PARSER:
    return exception_parser_to_chars(exc);
  case EXCEPTION_TREE:
    return exception_tree_to_chars(exc);
  case EXCEPTION_IR:
    return exception_ir_to_chars(exc);
  default:
    return exception_unknown_to_chars(exc);
  }
}

struct exception *
exception_new_from_helper(const struct exception_helper *helper, char *format,
                          ...) {
  va_list args;
  va_start(args, format);
  struct exception *exc =
      exception_new_v(helper->type, helper->subtype, helper->stream,
                      helper->line, helper->offset, helper->name, format, args);
  va_end(args);
  return exc;
}

const char *exception_subtype_ir_str(enum exception_subtype_ir type) {
  switch (type) {
  case EXCEPTION_IR_SUBROUTINE_REDEFINITION:
    return "subroutine redefinition";
  case EXCEPTION_IR_SUBROUTINE_OVERLOADING:
    return "subroutine overloading";
  case EXCEPTION_IR_SUBROUTINE_PARAMETER_DUPLICATION:
    return "subroutine parameter duplication";
  case EXCEPTION_IR_CHAR_VALIDATION:
    return "char validation";
  case EXCEPTION_IR_HEX_VALIDATION:
    return "hex validation";
  case EXCEPTION_IR_BITS_VALIDATION:
    return "bits validation";
  case EXCEPTION_IR_DEC_VALIDATION:
    return "dec validation";
  case EXCEPTION_IR_BOOL_VALIDATION:
    return "bool validation";
  case EXCEPTION_IR_RVALUE_ASSIGNMENT:
    return "rvalue assignment";
  case EXCEPTION_IR_NULL:
    return "null";
  case EXCEPTION_IR_SYMBOL_REDEFINITION:
    return "symbol redefinition";
  case EXCEPTION_IR_SYMBOL_UNDEFINED:
    return "symbol undefined";
  case EXCEPTION_IR_UNKNOWN:
  default:
    return "unknown";
  }
}
