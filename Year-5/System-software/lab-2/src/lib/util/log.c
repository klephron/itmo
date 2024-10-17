#include <stdarg.h>
#include <stdio.h>

int _debug(const char *fname, int lineno, const char *fxname, const char *fmt,
           ...) {
  printf("[debug %s:%d:%s]: ", fname, lineno, fxname);
  va_list args;
  va_start(args, fmt);
  vprintf(fmt, args);
  printf("\n");
  va_end(args);
  return 0;
}

int _info(const char *fname, int lineno, const char *fxname, const char *fmt,
          ...) {
  printf("[info %s:%d:%s]: ", fname, lineno, fxname);
  va_list args;
  va_start(args, fmt);
  vprintf(fmt, args);
  printf("\n");
  va_end(args);
  return 0;
}

int _error(const char *fname, int lineno, const char *fxname, const char *fmt,
           ...) {
  printf("[error %s:%d:%s]: ", fname, lineno, fxname);
  va_list args;
  va_start(args, fmt);
  vprintf(fmt, args);
  printf("\n");
  va_end(args);
  return 0;
}
