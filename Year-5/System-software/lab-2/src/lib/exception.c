#include "exception.h"

#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "util/colors.h"

static void exception_free(struct exception_struct *exc) {
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

struct exception_struct *exception_new(enum exception_type type, int subtype,
                                       const char *stream, uint32_t line,
                                       uint32_t offset, const char *name,
                                       const char *message) {

  struct exception_struct *exc = calloc(1, sizeof(struct exception_struct));
  exc->free = exception_free;

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

struct exception_struct *exception_new_v(enum exception_type type, int subtype,
                                         const char *stream, uint32_t line,
                                         uint32_t offset, const char *name,
                                         const char *format, va_list args) {
  char message[256]; // message size
  message[0] = '\0';
  vsnprintf(message, 255, format, args);
  return exception_new(type, subtype, stream, line, offset, name, message);
}

struct exception_struct *exception_new_f(enum exception_type type, int subtype,
                                         const char *stream, uint32_t line,
                                         uint32_t offset, const char *name,
                                         const char *format, ...) {
  struct exception_struct *exc;
  va_list args;
  va_start(args, format);
  exc =
      exception_new_v(type, subtype, stream, line, offset, name, format, args);
  va_end(args);
  return exc;
}

static char *exception_unknown_to_chars(const struct exception_struct *exc) {
  const char *format = ANSI_COLOR_RED "error" ANSI_COLOR_RESET
                                      "(U%d): %s -> %s\n" ANSI_COLOR_BLUE
                                      "  -->" ANSI_COLOR_RESET " %s:%d:%d";

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

  size_t len = 0;
  {
    len += strlen(format);
    len += strlen(exc_stream);
    len += strlen(exc_name);
    len += strlen(exc_message);
  }

  char *str = malloc(len + 1);
  str[0] = '\0';

  sprintf(str, format, exc->subtype, exc_name, exc_message, exc_stream,
          exc->line, exc->offset);

  assert(len >= strlen(str));

  return str;
}

static char *exception_lexer_to_chars(const struct exception_struct *exc) {
  const char *format =
      ANSI_COLOR_RED "error" ANSI_COLOR_RESET "(L%d): %s\n" ANSI_COLOR_BLUE
                     "  -->" ANSI_COLOR_RESET " %s:%d:%d";

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

  size_t len = 0;
  {
    len += strlen(format);
    len += strlen(exc_stream);
    len += strlen(exc_message);
  }

  char *str = malloc(len + 1);
  str[0] = '\0';

  sprintf(str, format, exc->subtype, exc_message, exc_stream, exc->line,
          exc->offset);

  assert(len >= strlen(str));

  return str;
}

static char *exception_parser_to_chars(const struct exception_struct *exc) {
  const char *format = ANSI_COLOR_RED "error" ANSI_COLOR_RESET
                                      "(P%d): %s -> %s\n" ANSI_COLOR_BLUE
                                      "  -->" ANSI_COLOR_RESET " %s:%d:%d";

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

  size_t len = 0;
  {
    len += strlen(format);
    len += strlen(exc_stream);
    len += strlen(exc_name);
    len += strlen(exc_message);
  }

  char *str = malloc(len + 1);
  str[0] = '\0';

  sprintf(str, format, exc->subtype, exc_name, exc_message, exc_stream,
          exc->line, exc->offset);

  assert(len >= strlen(str));

  return str;
}

static char *exception_tree_to_chars(const struct exception_struct *exc) {
  const char *format = ANSI_COLOR_RED "error" ANSI_COLOR_RESET
                                      "(T%d): %s -> %s\n" ANSI_COLOR_BLUE
                                      "  -->" ANSI_COLOR_RESET " %s";

  char *exc_stream = "<empty>";
  char *exc_message = "<empty>";
  char *exc_name = "<empty>";

  {
    if (exc->stream) {
      exc_stream = exc->stream;
    }

    if (exc->message) {
      exc_message = exc->message;
    }

    if (exc->name) {
      exc_name = exc->name;
    }
  }

  size_t len = 0;
  {
    len += strlen(format);
    len += strlen(exc_stream);
    len += strlen(exc_message);
    len += strlen(exc_name);
  }

  char *str = malloc(len + 1);
  str[0] = '\0';

  sprintf(str, format, exc->subtype, exc_name, exc_message, exc_stream,
          exc->line, exc->offset);

  assert(len >= strlen(str));

  return str;
}

static char *exception_ir_to_chars(const struct exception_struct *exc) {
  const char *format = ANSI_COLOR_RED "error" ANSI_COLOR_RESET
                                      "(I%d): %s -> %s\n" ANSI_COLOR_BLUE
                                      "  -->" ANSI_COLOR_RESET " %s:%d:%d";

  char *exc_stream = "<empty>";
  char *exc_message = "<empty>";
  char *exc_name = "<empty>";

  {
    if (exc->stream) {
      exc_stream = exc->stream;
    }

    if (exc->message) {
      exc_message = exc->message;
    }

    if (exc->name) {
      exc_name = exc->name;
    }
  }

  size_t len = 0;
  {
    len += strlen(format);
    len += strlen(exc_stream);
    len += strlen(exc_message);
    len += strlen(exc_name);
  }

  char *str = malloc(len + 1);
  str[0] = '\0';

  sprintf(str, format, exc->subtype, exc_name, exc_message, exc_stream,
          exc->line, exc->offset);

  assert(len >= strlen(str));

  return str;
}

char *exception_to_chars(const struct exception_struct *exc) {
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

struct exception_struct *
exception_new_from_helper(const struct exception_helper *helper, char *format,
                          ...) {
  va_list args;
  va_start(args, format);
  struct exception_struct *exc =
      exception_new_v(helper->type, helper->subtype, helper->stream,
                      helper->line, helper->offset, helper->name, format, args);
  va_end(args);
  return exc;
}
