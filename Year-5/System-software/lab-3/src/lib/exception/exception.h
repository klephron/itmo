#pragma once

#include <stdarg.h>
#include <stdint.h>

enum exception_type_enum {
  EXCEPTION_UNKNOWN,
  EXCEPTION_LEXER,
  EXCEPTION_PARSER,
  EXCEPTION_TREE,
  EXCEPTION_IR,
};

enum exception_subtype_lexer_enum {
  EXCEPTION_LEXER_UNKNOWN,
  EXCEPTION_LEXER_MATCHING,
};

enum exception_subtype_parser_enum {
  EXCEPTION_PARSER_UNKNOWN,
  EXCEPTION_PARSER_EOF,
  EXCEPTION_PARSER_GENERIC,
};

enum exception_subtype_tree_enum {
  EXCEPTION_TREE_UNKNOWN,
  EXCEPTION_TREE_UNEXPECTED_NODE,
};

enum exception_subtype_ir_enum {
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
};

struct exception_struct {
  enum exception_type_enum type;
  int                      subtype;
  char                    *stream;
  uint32_t                 line;
  uint32_t                 offset;
  char                    *name;
  char                    *message;
};

struct exception_helper_struct {
  enum exception_type_enum type;
  int                      subtype;
  char                    *stream;
  uint32_t                 line;
  uint32_t                 offset;
  char                    *name;
};

typedef enum exception_type_enum           exception_type;
typedef enum exception_subtype_lexer_enum  exception_subtype_lexer;
typedef enum exception_subtype_parser_enum exception_subtype_parser;
typedef enum exception_subtype_tree_enum   exception_subtype_tree;
typedef enum exception_subtype_ir_enum     exception_subtype_ir;

typedef struct exception_struct        exception;
typedef struct exception_helper_struct exception_helper;

exception *exception_new(exception_type type, int subtype, const char *stream,
                         uint32_t line, uint32_t offset, const char *name,
                         const char *message);
exception *exception_new_v(exception_type type, int subtype, const char *stream,
                           uint32_t line, uint32_t offset, const char *name,
                           const char *format, va_list args);
exception *exception_new_f(exception_type type, int subtype, const char *stream,
                           uint32_t line, uint32_t offset, const char *name,
                           const char *format, ...);
exception *exception_new_from_helper(const exception_helper *helper,
                                     char                   *format, ...);
void       exception_free(exception *exc);
char      *exception_to_chars(const exception *exc);

const char *exception_subtype_ir_str(exception_subtype_ir ir);
