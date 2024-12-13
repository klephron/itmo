#include "util/colors.h"
#include <stdarg.h>
#include <stdio.h>

int _debug(const char *fname, int lineno, const char *fxname, const char *fmt,
           ...) {
  printf(ANSI_COLOR_GRAY "[debug %s:%d:%s]: ", fname, lineno, fxname);
  va_list args;
  va_start(args, fmt);
  vprintf(fmt, args);
  printf(ANSI_COLOR_RESET "\n");
  va_end(args);
  return 0;
}

int _info(const char *fname, int lineno, const char *fxname, const char *fmt,
          ...) {
  printf(ANSI_COLOR_CYAN "[info %s:%d:%s]: ", fname, lineno, fxname);
  va_list args;
  va_start(args, fmt);
  vprintf(fmt, args);
  printf(ANSI_COLOR_RESET "\n");
  va_end(args);
  return 0;
}

int _warn(const char *fname, int lineno, const char *fxname, const char *fmt,
          ...) {
  printf(ANSI_COLOR_YELLOW "[warn %s:%d:%s]: ", fname, lineno, fxname);
  va_list args;
  va_start(args, fmt);
  vprintf(fmt, args);
  printf(ANSI_COLOR_RESET "\n");
  va_end(args);
  return 0;
}

int _error(const char *fname, int lineno, const char *fxname, const char *fmt,
           ...) {
  printf(ANSI_COLOR_RED "[error %s:%d:%s]: ", fname, lineno, fxname);
  va_list args;
  va_start(args, fmt);
  vprintf(fmt, args);
  printf(ANSI_COLOR_RESET "\n");
  va_end(args);
  return 0;
}
