#pragma once

#include <stdarg.h>
#include <stdint.h>

enum exception_type {
  EXCEPTION_UNKNOWN,
  EXCEPTION_LEXER,
  EXCEPTION_PARSER,
  EXCEPTION_TREE,
  EXCEPTION_IR,
};

enum exception_subtype_lexer {
  EXCEPTION_LEXER_UNKNOWN,
  EXCEPTION_LEXER_MATCHING,
};

enum exception_subtype_parser {
  EXCEPTION_PARSER_UNKNOWN,
  EXCEPTION_PARSER_EOF,
  EXCEPTION_PARSER_GENERIC,
};

enum exception_subtype_tree {
  EXCEPTION_TREE_UNKNOWN,
  EXCEPTION_TREE_UNEXPECTED_NODE,
};

enum exception_subtype_ir {
  EXCEPTION_IR_UNKNOWN,
  EXCEPTION_IR_FUNCTION_REDEFINITION,
  EXCEPTION_IR_FUNCTION_OVERLOADING,
  EXCEPTION_IR_FUNCTION_PARAMETER_DUPLICATION,
  EXCEPTION_IR_CHAR_VALIDATION,
  EXCEPTION_IR_HEX_VALIDATION,
  EXCEPTION_IR_BITS_VALIDATION,
  EXCEPTION_IR_DEC_VALIDATION,
  EXCEPTION_IR_BOOL_VALIDATION,
  EXCEPTION_IR_RVALUE_ASSIGNMENT,
  EXCEPTION_IR_NULL,
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
