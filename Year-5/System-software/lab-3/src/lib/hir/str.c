#include "str.h"
#include "span/str.h"
#include "type/str.h"
#include "util/math.h"
#include "util/string_buffer.h"
#include <string.h>

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

// HIR_SYMBOL
typedef struct hir_symbol_table_struct {
  char *name;
  char *bind_kind;
  char *bind_parent;
  char *type;
  char *span;
} hir_symbol_table;

void hir_symbol_table_deinit(hir_symbol_table *self) {
  if (self) {
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
    string_buffer_append(buffer, "::");
    string_buffer_append(buffer, parent->name);
    hir_binding_parent_str(buffer, parent->bind_parent);
  }
}

static hir_symbol_table hir_symbol_to_table(const hir_symbol *symbol) {
  char *type = type_str(symbol->type);

  string_buffer *bind_parent_buffer = string_buffer_new(16, 0);
  if (symbol->bind_parent) {
    hir_binding_parent_str(bind_parent_buffer, symbol->bind_parent);
  } else {
    string_buffer_append(bind_parent_buffer, "-");
  }

  return (hir_symbol_table){
      .name = strdup(symbol->name),
      .bind_kind = strdup(hir_binding_enum_str(symbol->bind_kind)),
      .bind_parent = string_buffer_detach(bind_parent_buffer),
      .type = type ? type : strdup("-"),
      .span = span_str(symbol->decl_span),
  };
}

// TODO: utf-8?
char *hir_list_symbol_str(const list_hir_symbol *symbols) {
  string_buffer *buffer = string_buffer_new(0, 0);

  const char *name_header = "NAME";
  const char *bind_kind_header = "BIND_KIND";
  const char *bind_parent_header = "BIND_PARENT";
  const char *type_header = "TYPE";
  const char *span_header = "SPAN";

  size_t name_ms = strlen(name_header);
  size_t bind_kind_ms = strlen(bind_kind_header);
  size_t bind_parent_ms = strlen(bind_parent_header);
  size_t type_ms = strlen(type_header);
  size_t span_ms = strlen(span_header);

  for (list_hir_symbol_it it =
           list_hir_symbol_begin((list_hir_symbol *)symbols);
       !list_hir_symbol_it_end(&it); list_hir_symbol_it_next(&it)) {
    const hir_symbol *symbol = list_hir_symbol_it_get(&it);
    hir_symbol_table  table = hir_symbol_to_table(symbol);

    size_t name_s = strlen(table.name);
    size_t bind_kind_s = strlen(table.bind_kind);
    size_t bind_parent_s = strlen(table.bind_parent);
    size_t type_s = strlen(table.type);
    size_t span_s = strlen(table.span);

    name_ms = size_t_max(name_ms, name_s);
    bind_kind_ms = size_t_max(bind_kind_ms, bind_kind_s);
    bind_parent_ms = size_t_max(bind_parent_ms, bind_parent_s);
    type_ms = size_t_max(type_ms, type_s);
    span_ms = size_t_max(span_ms, span_s);

    hir_symbol_table_deinit(&table);
  }

  // header
  {
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, name_header);
    for (size_t i = 0; i < name_ms - strlen(name_header); ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, bind_kind_header);
    for (size_t i = 0; i < bind_kind_ms - strlen(bind_kind_header); ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, bind_parent_header);
    for (size_t i = 0; i < bind_parent_ms - strlen(bind_parent_header); ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, type_header);
    for (size_t i = 0; i < type_ms - strlen(type_header); ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, span_header);
    for (size_t i = 0; i < span_ms - strlen(span_header); ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|\n");
  }

  // separator
  {
    string_buffer_append(buffer, "|");
    for (size_t i = 0; i < name_ms; ++i) {
      string_buffer_append(buffer, "-");
    }
    string_buffer_append(buffer, "|");
    for (size_t i = 0; i < bind_kind_ms; ++i) {
      string_buffer_append(buffer, "-");
    }
    string_buffer_append(buffer, "|");
    for (size_t i = 0; i < bind_parent_ms; ++i) {
      string_buffer_append(buffer, "-");
    }
    string_buffer_append(buffer, "|");
    for (size_t i = 0; i < type_ms; ++i) {
      string_buffer_append(buffer, "-");
    }
    string_buffer_append(buffer, "|");
    for (size_t i = 0; i < span_ms; ++i) {
      string_buffer_append(buffer, "-");
    }
    string_buffer_append(buffer, "|\n");
  }

  // body
  for (list_hir_symbol_it it =
           list_hir_symbol_begin((list_hir_symbol *)symbols);
       !list_hir_symbol_it_end(&it); list_hir_symbol_it_next(&it)) {
    const hir_symbol *symbol = list_hir_symbol_it_get(&it);
    hir_symbol_table  table = hir_symbol_to_table(symbol);

    size_t name_s = strlen(table.name);
    size_t bind_kind_s = strlen(table.bind_kind);
    size_t bind_parent_s = strlen(table.bind_parent);
    size_t type_s = strlen(table.type);
    size_t span_s = strlen(table.span);

    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, table.name);
    for (size_t i = 0; i < name_ms - name_s; ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, table.bind_kind);
    for (size_t i = 0; i < bind_kind_ms - bind_kind_s; ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, table.bind_parent);
    for (size_t i = 0; i < bind_parent_ms - bind_parent_s; ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, table.type);
    for (size_t i = 0; i < type_ms - type_s; ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|");
    string_buffer_append(buffer, table.span);
    for (size_t i = 0; i < span_ms - span_s; ++i) {
      string_buffer_append(buffer, " ");
    }
    string_buffer_append(buffer, "|\n");

    hir_symbol_table_deinit(&table);
  }

  return string_buffer_detach(buffer);
}
