#pragma once

#include <stdarg.h>
#include <stdint.h>

enum exception_type {
  EXCEPTION_UNKNOWN = 0,
  EXCEPTION_LEXER = 1,
  EXCEPTION_PARSER = 2,
  EXCEPTION_IR = 3,
};

enum exception_subtype_lexer {
  EXCEPTION_LEXER_UNKNOWN = 0,
  EXCEPTION_LEXER_MATCHING = 1,
};

enum exception_subtype_parser {
  EXCEPTION_PARSER_UNKNOWN = 0,
  EXCEPTION_PARSER_EOF = 1,
  EXCEPTION_PARSER_GENERIC = 2,
};

enum exception_subtype_ir {
  EXCEPTION_IR_UNKNOWN = 0,
  EXCEPTION_IR_FUNCTION_REDEFINITION = 1,
  EXCEPTION_IR_FUNCTION_OVERLOADING = 2,
};

struct exception_struct {
  enum exception_type type;
  int subtype;
  char *stream;
  uint32_t line;
  uint32_t offset;
  char *name;
  char *message;

  void (*free)(struct exception_struct *);
};

typedef struct exception_struct exception;

struct exception_struct *exception_new(enum exception_type type, int subtype,
                                       const char *stream, uint32_t line,
                                       uint32_t offset, const char *name,
                                       const char *message);
struct exception_struct *exception_new_v(enum exception_type type, int subtype,
                                         const char *stream, uint32_t line,
                                         uint32_t offset, const char *name,
                                         const char *format, va_list args);
struct exception_struct *exception_new_f(enum exception_type type, int subtype,
                                         const char *stream, uint32_t line,
                                         uint32_t offset, const char *name,
                                         const char *format, ...);

char *exception_to_chars(const struct exception_struct *exc);
