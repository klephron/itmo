#pragma once

#include <stdarg.h>
#include <stdint.h>

enum exception_type {
  EXCEPTION_UNKNOWN = 0,
  EXCEPTION_LEXER = 1,
  EXCEPTION_PARSER = 2,
  EXCEPTION_TREE = 3,
  EXCEPTION_IR = 4,
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

enum exception_subtype_tree {
  EXCEPTION_TREE_UNKNOWN = 0,
  EXCEPTION_TREE_UNEXPECTED_NODE = 1,
};

enum exception_subtype_ir {
  EXCEPTION_IR_UNKNOWN = 0,
  EXCEPTION_IR_FUNCTION_REDEFINITION = 1,
  EXCEPTION_IR_FUNCTION_OVERLOADING = 2,
  EXCEPTION_IR_FUNCTION_PARAMETER_DUPLICATION = 3,
  EXCEPTION_IR_CHAR_VALIDATION = 4,
  EXCEPTION_IR_HEX_VALIDATION = 5,
  EXCEPTION_IR_BITS_VALIDATION = 6,
  EXCEPTION_IR_DEC_VALIDATION = 7,
  EXCEPTION_IR_BOOL_VALIDATION = 8,
};

struct exception {
  enum exception_type type;
  int                 subtype;
  char               *stream;
  uint32_t            line;
  uint32_t            offset;
  char               *name;
  char               *message;
};

struct exception_helper {
  enum exception_type type;
  int                 subtype;
  char               *stream;
  uint32_t            line;
  uint32_t            offset;
  char               *name;
};

typedef struct exception        exception;
typedef struct exception_helper exception_helper;

exception        *exception_new(enum exception_type type, int subtype,
                                const char *stream, uint32_t line, uint32_t offset,
                                const char *name, const char *message);
exception        *exception_new_v(enum exception_type type, int subtype,
                                  const char *stream, uint32_t line, uint32_t offset,
                                  const char *name, const char *format, va_list args);
struct exception *exception_new_f(enum exception_type type, int subtype,
                                  const char *stream, uint32_t line,
                                  uint32_t offset, const char *name,
                                  const char *format, ...);

void exception_free(exception *exc);

char *exception_to_chars(const struct exception *exc);

struct exception *
exception_new_from_helper(const struct exception_helper *helper, char *format,
                          ...);
