#include "exception.h"


#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "util/colors.h"

static void exception_free(struct exception_struct *exc) {
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

struct exception_struct *exception_new(const char *stream, uint32_t type,
                                              uint32_t line, uint32_t offset, const char *name,
                                              const char *message) {

  struct exception_struct *exc = calloc(1, sizeof(struct exception_struct));

  exc->free = exception_free;

  if (stream) {
    exc->stream = strdup(stream);
  }

  exc->type = type;
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

// <file>: <name> -> <message> \n
char *exception_to_chars(struct exception_struct *exc) {
  const char *format_print = "%s: " ANSI_COLOR_RED "%s" ANSI_COLOR_RESET " -> %s\n";
  const char *format = ": " ANSI_COLOR_RED ANSI_COLOR_RESET " -> \n";

  char *exc_stream = "<empty>";
  uint32_t exc_line;
  uint32_t exc_offset;
  char *exc_name = "<empty>";
  char *exc_message = "<empty>";

  {
    if (exc->stream) {
      exc_stream = exc->stream;
    }

    exc_line = exc->line;
    exc_offset = exc->offset;

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

    char buf[32];
    buf[0] = '\0';

  }

  char *str = malloc(len + 1);
  str[0] = '\0';

  sprintf(str, format_print, exc_stream, exc_name, exc_message);

  assert(len == strlen(str));

  return str;
}
