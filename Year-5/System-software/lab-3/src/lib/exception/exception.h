#pragma once

#include <stdarg.h>
#include <stdint.h>

typedef enum exception_type_enum {
  EXCEPTION_UNKNOWN,
  EXCEPTION_LEXER,
  EXCEPTION_PARSER,
  EXCEPTION_TREE,
  EXCEPTION_IR,
} exception_type;

typedef enum exception_level_enum {
  EXCEPTION_LEVEL_UNKNOWN,
  EXCEPTION_LEVEL_INFO,
  EXCEPTION_LEVEL_WARNING,
  EXCEPTION_LEVEL_ERROR,
} exception_level;

typedef enum exception_subtype_lexer_enum {
  EXCEPTION_LEXER_UNKNOWN,
  EXCEPTION_LEXER_MATCHING,
} exception_subtype_lexer;

typedef enum exception_subtype_parser_enum {
  EXCEPTION_PARSER_UNKNOWN,
  EXCEPTION_PARSER_EOF,
  EXCEPTION_PARSER_GENERIC,
} exception_subtype_parser;

typedef enum exception_subtype_tree_enum {
  EXCEPTION_TREE_UNKNOWN,
  EXCEPTION_TREE_UNEXPECTED_NODE,
} exception_subtype_tree;

typedef enum exception_subtype_ir_enum {
  EXCEPTION_IR_UNKNOWN,
  EXCEPTION_IR_SUBROUTINE_REDEFINITION,
  EXCEPTION_IR_SUBROUTINE_OVERLOADING,
  EXCEPTION_IR_SUBROUTINE_PARAMETER_DUPLICATION,
  EXCEPTION_IR_CHAR_VALIDATION,
  EXCEPTION_IR_HEX_VALIDATION,
  EXCEPTION_IR_BITS_VALIDATION,
  EXCEPTION_IR_DEC_VALIDATION,
  EXCEPTION_IR_BOOL_VALIDATION,
  EXCEPTION_IR_RVALUE_ASSIGNMENT,
  EXCEPTION_IR_NULL,
  EXCEPTION_IR_SYMBOL_REDEFINITION,
  EXCEPTION_IR_SYMBOL_UNDEFINED,
} exception_subtype_ir;

typedef struct exception_struct {
  exception_level level;
  exception_type  type;
  int             subtype;
  char           *stream;
  uint32_t        line;
  uint32_t        offset;
  char           *name;
  char           *message;
} exception;

typedef struct exception_helper_struct {
  exception_level level;
  exception_type  type;
  const char     *name;
  int             subtype;
  uint32_t        line;
  uint32_t        offset;
  const char     *stream;
} exception_helper;

exception *exception_new(exception_level level, exception_type type,
                         int subtype, const char *stream, uint32_t line,
                         uint32_t offset, const char *name,
                         const char *message);
exception *exception_new_v(exception_level level, exception_type type,
                           int subtype, const char *stream, uint32_t line,
                           uint32_t offset, const char *name,
                           const char *format, va_list args);
exception *exception_new_f(exception_level level, exception_type type,
                           int subtype, const char *stream, uint32_t line,
                           uint32_t offset, const char *name,
                           const char *format, ...);
exception *exception_new_from_helper(const exception_helper *helper,
                                     char                   *format, ...);
void       exception_free(exception *exc);
char      *exception_str(const exception *exc);

const char *exception_subtype_ir_str(exception_subtype_ir ir);
