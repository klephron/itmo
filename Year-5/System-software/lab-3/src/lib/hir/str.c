#include "str.h"
#include "span/str.h"
#include "type/str.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/math.h"
#include "util/string_buffer.h"
#include <stdio.h>
#include <string.h>

#define HIR_STR_TAB 2
#define HIR_STR_SPACE " "

static void hir_tree_str_stmt_if(string_buffer *buffer, size_t pad,
                                 hir_stmt_if *stmt);
static void hir_tree_str_stmt_block(string_buffer *buffer, size_t pad,
                                    hir_stmt_block *stmt);
static void hir_tree_str_stmt_while(string_buffer *buffer, size_t pad,
                                    hir_stmt_while *stmt);
static void hir_tree_str_stmt_do(string_buffer *buffer, size_t pad,
                                 hir_stmt_do *stmt);
static void hir_tree_str_stmt_break(string_buffer *buffer, size_t pad,
                                    hir_stmt_break *stmt);
static void hir_tree_str_stmt_expr(string_buffer *buffer, size_t pad,
                                   hir_stmt_expr *stmt);

static void hir_tree_str_expr_unary(string_buffer *buffer, size_t pad,
                                    hir_expr_unary *expr);
static void hir_tree_str_expr_binary(string_buffer *buffer, size_t pad,
                                     hir_expr_binary *expr);
static void hir_tree_str_expr_lit(string_buffer *buffer, size_t pad,
                                  hir_expr_lit *expr);
static void hir_tree_str_expr_id(string_buffer *buffer, size_t pad,
                                 hir_expr_id *expr);
static void hir_tree_str_expr_call(string_buffer *buffer, size_t pad,
                                   hir_expr_call *expr);
static void hir_tree_str_expr_index(string_buffer *buffer, size_t pad,
                                    hir_expr_index *expr);

// EXPRESSION_ENUM
const char *hir_expr_unary_enum_str(hir_expr_unary_enum type) {
  switch (type) {
  case HIR_EXPR_UNARY_PLUS:
    return "PLUS";
  case HIR_EXPR_UNARY_MINUS:
    return "MINUS";
  case HIR_EXPR_UNARY_LOGICAL_NOT:
    return "LOGICAL_NOT";
  case HIR_EXPR_UNARY_BITWISE_NOT:
    return "BITWISE_NOT";
  case HIR_EXPR_UNARY_INC:
    return "INC";
  case HIR_EXPR_UNARY_DEC:
    return "DEC";
  default:
    return "UNKNOWN";
  }
}

const char *hir_expr_binary_enum_str(hir_expr_binary_enum type) {
  switch (type) {
  case HIR_EXPR_BINARY_ASSIGN:
    return "ASSIGN";
  case HIR_EXPR_BINARY_LOGICAL_OR:
    return "LOGICAL_OR";
  case HIR_EXPR_BINARY_LOGICAL_AND:
    return "LOGICAL_AND";
  case HIR_EXPR_BINARY_BITWISE_OR:
    return "BITWISE_OR";
  case HIR_EXPR_BINARY_BITWISE_XOR:
    return "BITWISE_XOR";
  case HIR_EXPR_BINARY_BITWISE_AND:
    return "BITWISE_AND";
  case HIR_EXPR_BINARY_EQUALS:
    return "EQUALS";
  case HIR_EXPR_BINARY_NOT_EQUALS:
    return "NOT_EQUALS";
  case HIR_EXPR_BINARY_LESS:
    return "LESS";
  case HIR_EXPR_BINARY_LESS_EQUALS:
    return "LESS_EQUALS";
  case HIR_EXPR_BINARY_GREATER:
    return "GREATER";
  case HIR_EXPR_BINARY_GREATER_EQUALS:
    return "GREATER_EQUALS";
  case HIR_EXPR_BINARY_BITWISE_SHIFT_LEFT:
    return "BITWISE_SHIFT_LEFT";
  case HIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT:
    return "BITWISE_SHIFT_RIGHT";
  case HIR_EXPR_BINARY_ADD:
    return "ADD";
  case HIR_EXPR_BINARY_SUB:
    return "SUB";
  case HIR_EXPR_BINARY_MUL:
    return "MUL";
  case HIR_EXPR_BINARY_DIV:
    return "DIV";
  case HIR_EXPR_BINARY_MOD:
    return "MOD";
  default:
    return "UNKNOWN";
  }
}

const char *hir_node_enum_str(hir_node_enum kind) {
  switch (kind) {
  case HIR_NODE_ID:
    return "ID";
  case HIR_NODE_LIT:
    return "LIT";
  case HIR_NODE_PARAM:
    return "PARAM";
  case HIR_NODE_VAR:
    return "VAR";
  case HIR_NODE_SUBROUTINE:
    return "SUBROUTINE";
  case HIR_NODE_STMT:
    return "STMT";
  case HIR_NODE_EXPR:
    return "EXPR";
  default:
    return "UNKNOWN";
  }
}

const char *hir_stmt_enum_str(hir_stmt_enum kind) {
  switch (kind) {
  case HIR_STMT_IF:
    return "IF";
  case HIR_STMT_BLOCK:
    return "BLOCK";
  case HIR_STMT_WHILE:
    return "WHILE";
  case HIR_STMT_DO:
    return "DO";
  case HIR_STMT_BREAK:
    return "BREAK";
  case HIR_STMT_EXPR:
    return "EXPR";
  default:
    return "UNKNOWN";
  }
}

const char *hir_expr_enum_str(hir_expr_enum kind) {
  switch (kind) {
  case HIR_EXPR_UNARY:
    return "UNARY";
  case HIR_EXPR_BINARY:
    return "BINARY";
  case HIR_EXPR_LITERAL:
    return "LITERAL";
  case HIR_EXPR_IDENTIFIER:
    return "IDENTIFIER";
  case HIR_EXPR_CALL:
    return "CALL";
  case HIR_EXPR_INDEX:
    return "INDEX";
  default:
    return "UNKNOWN";
  }
}

char *hir_lit_value_str(const hir_lit *lit) {
  switch (lit->type->type) {
  case TYPE_STRING:
    return strdup(lit->value.v_str);
  case TYPE_CHAR:
    return strdup(lit->value.v_char);
  case TYPE_BOOL: {
    if (lit->value.v_bool) {
      return strdup("true");
    } else {
      return strdup("false");
    }
  } break;
  case TYPE_VOID:
    return strdup("(nothing)");
  case TYPE_ARRAY:
  case TYPE_CUSTOM:
  case TYPE_CALLABLE:
    return strdup("(no info)");
  case TYPE_BYTE:
  case TYPE_INT:
  case TYPE_LONG: {
    char *buf = MALLOCN(char, 32);
    snprintf(buf, 31, "%ld", lit->value.v_long);
    return buf;
  }
  case TYPE_UINT:
  case TYPE_ULONG: {
    char *buf = MALLOCN(char, 32);
    snprintf(buf, 31, "%lu", lit->value.v_ulong);
    return buf;
  }
  default:
    error("unknown lit type %d", lit->type->type);
    return strdup("(unknown)");
  }
}

// HIR_SYMBOL
typedef struct hir_symbol_table_struct {
  char *id_ptr;
  char *name;
  char *bind_kind;
  char *bind_parent;
  char *type;
  char *span;
} hir_symbol_table;

static void hir_symbol_table_deinit(hir_symbol_table *self) {
  if (self) {
    free(self->id_ptr);
    free(self->name);
    free(self->bind_kind);
    free(self->bind_parent);
    free(self->type);
    free(self->span);
  }
}

const char *hir_binding_enum_str(hir_binding_enum kind) {
  switch (kind) {
  case HIR_BINDING_GLOBAL:
    return "GLOBAL";
  case HIR_BINDING_SUBROUTINE:
    return "SUBROUTINE";
  default:
    return "UNKNOWN";
  }
}

static void hir_binding_parent_str(string_buffer    *buffer,
                                   const hir_symbol *parent) {
  if (parent) {
    hir_binding_parent_str(buffer, parent->bind_parent);
    string_buffer_append(buffer, "::");
    string_buffer_append(buffer, parent->name);
  }
}

static hir_symbol_table hir_symbol_to_table(const hir_symbol *symbol) {
  char id_ptr[16];
  snprintf(id_ptr, STRMAXLEN(id_ptr), "%p", symbol);

  string_buffer *bind_parent_buffer = string_buffer_new(16, 0);
  if (symbol->bind_parent) {
    hir_binding_parent_str(bind_parent_buffer, symbol->bind_parent);
  } else {
    string_buffer_append(bind_parent_buffer, "-");
  }

  char *type = type_str(symbol->type);

  return (hir_symbol_table){
      .id_ptr = strdup(id_ptr),
      .name = strdup(symbol->name),
      .bind_kind = strdup(hir_binding_enum_str(symbol->bind_kind)),
      .bind_parent = string_buffer_detach(bind_parent_buffer),
      .type = type ? type : strdup("-"),
      .span = span_str(symbol->decl_span),
  };
}

static inline void string_buffer_append_cell(string_buffer *buffer,
                                             size_t max_len, const char *field,
                                             const char *space) {
  string_buffer_append(buffer, field);
  for (size_t i = 0; i < max_len - strlen(field); ++i) {
    string_buffer_append(buffer, space);
  }
}

// TODO: utf-8?
char *hir_list_symbol_str(const list_hir_symbol *symbols) {
  string_buffer *buffer = string_buffer_new(0, 0);

  const char *id_h = "ID(PTR)";
  const char *name_h = "NAME";
  const char *bind_kind_h = "BIND_KIND";
  const char *bind_parent_h = "BIND_PARENT";
  const char *type_h = "TYPE";
  const char *span_h = "SPAN";

  size_t id_ms = strlen(id_h);
  size_t name_ms = strlen(name_h);
  size_t bind_kind_ms = strlen(bind_kind_h);
  size_t bind_parent_ms = strlen(bind_parent_h);
  size_t type_ms = strlen(type_h);
  size_t span_ms = strlen(span_h);

  for (list_hir_symbol_it it =
           list_hir_symbol_begin((list_hir_symbol *)symbols);
       !it.end(&it); it.next(&it)) {
    const hir_symbol *symbol = it.get(&it);
    hir_symbol_table  table = hir_symbol_to_table(symbol);

    size_t id_s = strlen(table.id_ptr);
    size_t name_s = strlen(table.name);
    size_t bind_kind_s = strlen(table.bind_kind);
    size_t bind_parent_s = strlen(table.bind_parent);
    size_t type_s = strlen(table.type);
    size_t span_s = strlen(table.span);

    id_ms = max_size_t(id_ms, id_s);
    name_ms = max_size_t(name_ms, name_s);
    bind_kind_ms = max_size_t(bind_kind_ms, bind_kind_s);
    bind_parent_ms = max_size_t(bind_parent_ms, bind_parent_s);
    type_ms = max_size_t(type_ms, type_s);
    span_ms = max_size_t(span_ms, span_s);

    hir_symbol_table_deinit(&table);
  }

  // separator
  {
    string_buffer_append(buffer, "┌");
    string_buffer_append_cell(buffer, id_ms, "", "─");
    string_buffer_append(buffer, "┬");
    string_buffer_append_cell(buffer, name_ms, "", "─");
    string_buffer_append(buffer, "┬");
    string_buffer_append_cell(buffer, bind_kind_ms, "", "─");
    string_buffer_append(buffer, "┬");
    string_buffer_append_cell(buffer, bind_parent_ms, "", "─");
    string_buffer_append(buffer, "┬");
    string_buffer_append_cell(buffer, type_ms, "", "─");
    string_buffer_append(buffer, "┬");
    string_buffer_append_cell(buffer, span_ms, "", "─");
    string_buffer_append(buffer, "┐\n");
  }

  // header
  {
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, id_ms, id_h, HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, name_ms, name_h, HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, bind_kind_ms, bind_kind_h, HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, bind_parent_ms, bind_parent_h,
                              HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, type_ms, type_h, HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, span_ms, span_h, HIR_STR_SPACE);
    string_buffer_append(buffer, "│\n");
  }

  // separator
  {
    string_buffer_append(buffer, "├");
    string_buffer_append_cell(buffer, id_ms, "", "─");
    string_buffer_append(buffer, "┼");
    string_buffer_append_cell(buffer, name_ms, "", "─");
    string_buffer_append(buffer, "┼");
    string_buffer_append_cell(buffer, bind_kind_ms, "", "─");
    string_buffer_append(buffer, "┼");
    string_buffer_append_cell(buffer, bind_parent_ms, "", "─");
    string_buffer_append(buffer, "┼");
    string_buffer_append_cell(buffer, type_ms, "", "─");
    string_buffer_append(buffer, "┼");
    string_buffer_append_cell(buffer, span_ms, "", "─");
    string_buffer_append(buffer, "┤\n");
  }

  // body
  for (list_hir_symbol_it it =
           list_hir_symbol_begin((list_hir_symbol *)symbols);
       !it.end(&it); it.next(&it)) {
    const hir_symbol *symbol = it.get(&it);
    hir_symbol_table  table = hir_symbol_to_table(symbol);

    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, id_ms, table.id_ptr, HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, name_ms, table.name, HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, bind_kind_ms, table.bind_kind,
                              HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, bind_parent_ms, table.bind_parent,
                              HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, type_ms, table.type, HIR_STR_SPACE);
    string_buffer_append(buffer, "│");
    string_buffer_append_cell(buffer, span_ms, table.span, HIR_STR_SPACE);
    string_buffer_append(buffer, "│\n");

    hir_symbol_table_deinit(&table);
  }

  // separator
  {
    string_buffer_append(buffer, "└");
    string_buffer_append_cell(buffer, id_ms, "", "─");
    string_buffer_append(buffer, "┴");
    string_buffer_append_cell(buffer, name_ms, "", "─");
    string_buffer_append(buffer, "┴");
    string_buffer_append_cell(buffer, bind_kind_ms, "", "─");
    string_buffer_append(buffer, "┴");
    string_buffer_append_cell(buffer, bind_parent_ms, "", "─");
    string_buffer_append(buffer, "┴");
    string_buffer_append_cell(buffer, type_ms, "", "─");
    string_buffer_append(buffer, "┴");
    string_buffer_append_cell(buffer, span_ms, "", "─");
    string_buffer_append(buffer, "┘\n");
  }

  return string_buffer_detach(buffer);
}

// HIR TREE
static void string_buffer_append_padding(string_buffer *buffer, size_t size) {
  for (size_t i = 0; i < size; ++i) {
    string_buffer_append(buffer, HIR_STR_SPACE);
  }
}

static void string_buffer_appendln(string_buffer *buffer, const char *data) {
  string_buffer_append(buffer, data);
  string_buffer_append(buffer, "\n");
}

static void hir_tree_str_span(string_buffer *buffer, size_t pad,
                              const span *span) {
  UNUSED(pad);
  char *s = span_str(span);
  string_buffer_append(buffer, s);
  free(s);
}

static void hir_tree_str_type(string_buffer *buffer, size_t pad,
                              const type_base *type) {
  UNUSED(pad);
  char *s = type_str(type);
  if (s) {
    string_buffer_append(buffer, s);
    free(s);
  } else {
    string_buffer_append(buffer, "(nil)");
  }
}

static void hir_tree_str_base(string_buffer *buffer, size_t pad,
                              const hir_base *base) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "kind: ");
  string_buffer_appendln(buffer, hir_node_enum_str(base->kind));

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "span: ");
  hir_tree_str_span(buffer, pad, base->span);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_id(string_buffer *buffer, size_t pad,
                            const hir_id *id) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_base(buffer, pad, &id->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "name: ");
  string_buffer_appendln(buffer, id->name);

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_symbol(string_buffer *buffer, size_t pad,
                                const hir_symbol *symbol) {
  UNUSED(pad);
  char buf[16];
  snprintf(buf, STRMAXLEN(buf), "%p", symbol);
  string_buffer_append(buffer, buf);
}

static void hir_tree_str_lit(string_buffer *buffer, size_t pad,
                             const hir_lit *lit) {
  if (!lit) {
    string_buffer_append(buffer, "(nil)");
    return;
  }

  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_base(buffer, pad, &lit->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "type: ");
  hir_tree_str_type(buffer, pad, lit->type);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "value: ");
  char *s = hir_lit_value_str(lit);
  string_buffer_appendln(buffer, s);
  free(s);

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_param(string_buffer *buffer, size_t pad,
                               const hir_param *param) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_base(buffer, pad, &param->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "id: ");
  hir_tree_str_id(buffer, pad, param->id);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "type: ");
  hir_tree_str_type(buffer, pad, param->type);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "symbol: ");
  hir_tree_str_symbol(buffer, pad, param->symbol);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_var(string_buffer *buffer, size_t pad,
                             const hir_var *var) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_base(buffer, pad, &var->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "id: ");
  hir_tree_str_id(buffer, pad, var->id);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "type: ");
  hir_tree_str_type(buffer, pad, var->type);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "symbol: ");
  hir_tree_str_symbol(buffer, pad, var->symbol);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr_base(string_buffer *buffer, size_t pad,
                                   hir_expr_base *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "kind: ");
  string_buffer_appendln(buffer, hir_expr_enum_str(expr->kind));

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "type: ");
  hir_tree_str_type(buffer, pad, expr->type);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr(string_buffer *buffer, size_t pad,
                              hir_expr_base *expr) {
  if (!expr) {
    string_buffer_append(buffer, "(nil)");
  }
  switch (expr->kind) {
  case HIR_EXPR_UNARY:
    return hir_tree_str_expr_unary(buffer, pad, (hir_expr_unary *)expr);
  case HIR_EXPR_BINARY:
    return hir_tree_str_expr_binary(buffer, pad, (hir_expr_binary *)expr);
  case HIR_EXPR_LITERAL:
    return hir_tree_str_expr_lit(buffer, pad, (hir_expr_lit *)expr);
  case HIR_EXPR_IDENTIFIER:
    return hir_tree_str_expr_id(buffer, pad, (hir_expr_id *)expr);
  case HIR_EXPR_CALL:
    return hir_tree_str_expr_call(buffer, pad, (hir_expr_call *)expr);
  case HIR_EXPR_INDEX:
    return hir_tree_str_expr_index(buffer, pad, (hir_expr_index *)expr);
  default:
    error("unknown expr kind %d", expr->kind);
  }
}

static void hir_tree_str_expr_unary(string_buffer *buffer, size_t pad,
                                    hir_expr_unary *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_expr_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "op: ");
  string_buffer_appendln(buffer, hir_expr_unary_enum_str(expr->op));

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "first: ");
  hir_tree_str_expr(buffer, pad, expr->first);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr_binary(string_buffer *buffer, size_t pad,
                                     hir_expr_binary *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_expr_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "op: ");
  string_buffer_appendln(buffer, hir_expr_binary_enum_str(expr->op));

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "first: ");
  hir_tree_str_expr(buffer, pad, expr->first);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "second: ");
  hir_tree_str_expr(buffer, pad, expr->second);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr_lit(string_buffer *buffer, size_t pad,
                                  hir_expr_lit *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_expr_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "lit: ");
  hir_tree_str_lit(buffer, pad, expr->lit);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr_id(string_buffer *buffer, size_t pad,
                                 hir_expr_id *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_expr_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "id: ");
  hir_tree_str_id(buffer, pad, expr->id);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "binding: ");
  hir_tree_str_symbol(buffer, pad, expr->binding);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr_call(string_buffer *buffer, size_t pad,
                                   hir_expr_call *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_expr_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "callee: ");
  hir_tree_str_expr(buffer, pad, expr->callee);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "args: [");
  pad += HIR_STR_TAB;
  for (list_hir_expr_it it = list_hir_expr_begin(expr->args); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    hir_tree_str_expr(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "]");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr_index(string_buffer *buffer, size_t pad,
                                    hir_expr_index *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_expr_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "indexed: ");
  hir_tree_str_expr(buffer, pad, expr->indexed);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "args: [");
  pad += HIR_STR_TAB;
  for (list_hir_expr_it it = list_hir_expr_begin(expr->args); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    hir_tree_str_expr(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "]");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_stmt_base(string_buffer *buffer, size_t pad,
                                   hir_stmt_base *stmt) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_base(buffer, pad, &stmt->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "kind: ");
  string_buffer_appendln(buffer, hir_stmt_enum_str(stmt->kind));

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_stmt(string_buffer *buffer, size_t pad,
                              hir_stmt_base *stmt) {
  if (!stmt) {
    string_buffer_append(buffer, "(nil)");
  }
  switch (stmt->kind) {
  case HIR_STMT_IF:
    return hir_tree_str_stmt_if(buffer, pad, (hir_stmt_if *)stmt);
  case HIR_STMT_BLOCK:
    return hir_tree_str_stmt_block(buffer, pad, (hir_stmt_block *)stmt);
  case HIR_STMT_WHILE:
    return hir_tree_str_stmt_while(buffer, pad, (hir_stmt_while *)stmt);
  case HIR_STMT_DO:
    return hir_tree_str_stmt_do(buffer, pad, (hir_stmt_do *)stmt);
  case HIR_STMT_BREAK:
    return hir_tree_str_stmt_break(buffer, pad, (hir_stmt_break *)stmt);
  case HIR_STMT_EXPR:
    return hir_tree_str_stmt_expr(buffer, pad, (hir_stmt_expr *)stmt);
  default:
    error("unknown stmt kind %d", stmt->kind);
  }
}

static void hir_tree_str_stmt_if(string_buffer *buffer, size_t pad,
                                 hir_stmt_if *stmt) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_stmt_base(buffer, pad, &stmt->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "cond: ");
  hir_tree_str_expr(buffer, pad, stmt->cond);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "je: ");
  hir_tree_str_stmt(buffer, pad, stmt->je);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "jz: ");
  hir_tree_str_stmt(buffer, pad, stmt->jz);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_stmt_block(string_buffer *buffer, size_t pad,
                                    hir_stmt_block *stmt) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_stmt_base(buffer, pad, &stmt->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "stmts: [");
  pad += HIR_STR_TAB;
  for (list_hir_stmt_it it = list_hir_stmt_begin(stmt->stmts); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    hir_tree_str_stmt(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "]");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_stmt_while(string_buffer *buffer, size_t pad,
                                    hir_stmt_while *stmt) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_stmt_base(buffer, pad, &stmt->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "cond: ");
  hir_tree_str_expr(buffer, pad, stmt->cond);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "stmt: ");
  hir_tree_str_stmt(buffer, pad, stmt->stmt);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_stmt_do(string_buffer *buffer, size_t pad,
                                 hir_stmt_do *stmt) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_stmt_base(buffer, pad, &stmt->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "positive: ");
  char buf[16];
  if (stmt->positive) {
    sprintf(buf, "true");
  } else {
    sprintf(buf, "false");
  }
  string_buffer_appendln(buffer, buf);

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "cond: ");
  hir_tree_str_expr(buffer, pad, stmt->cond);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "stmt: ");
  hir_tree_str_stmt(buffer, pad, stmt->stmt);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_stmt_break(string_buffer *buffer, size_t pad,
                                    hir_stmt_break *stmt) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_stmt_base(buffer, pad, &stmt->base);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_stmt_expr(string_buffer *buffer, size_t pad,
                                   hir_stmt_expr *stmt) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_stmt_base(buffer, pad, &stmt->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "expr: ");
  hir_tree_str_expr(buffer, pad, stmt->expr);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_subroutine(string_buffer *buffer, size_t pad,
                                    const hir_subroutine *subroutine) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_base(buffer, pad, &subroutine->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "id: ");
  hir_tree_str_id(buffer, pad, subroutine->id);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "params: [");
  pad += HIR_STR_TAB;
  for (list_hir_param_it it = list_hir_param_begin(subroutine->params);
       !it.end(&it); it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    hir_tree_str_param(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "]");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "ret_type: ");
  hir_tree_str_type(buffer, pad, subroutine->ret_type);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "vars: [");
  pad += HIR_STR_TAB;
  for (list_hir_var_it it = list_hir_var_begin(subroutine->vars); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    hir_tree_str_var(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "]");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "body: ");
  hir_tree_str_stmt_block(buffer, pad, subroutine->body);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "binding: ");
  hir_tree_str_symbol(buffer, pad, subroutine->binding);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

char *hir_tree_str(const hir *hir) {
  string_buffer *buffer = string_buffer_new(0, 0);
  string_buffer_appendln(buffer, "{");
  size_t pad = HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "SUBROUTINES: [");
  pad += HIR_STR_TAB;
  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !it.end(&it); it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    hir_tree_str_subroutine(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, HIR_STR_TAB);
  string_buffer_appendln(buffer, "]");

  string_buffer_append(buffer, "}");
  return string_buffer_detach(buffer);
}
