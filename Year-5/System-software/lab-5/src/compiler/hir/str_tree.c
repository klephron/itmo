#include "compiler/span/str.h"
#include "str.h"

#include "util/log.h"
#include "util/macro.h"
#include "util/string_buffer.h"
#include <stdio.h>

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
static void hir_tree_str_expr_builtin(string_buffer *buffer, size_t pad,
                                      hir_expr_builtin *expr);

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
                              const hir_type_base *type) {
  UNUSED(pad);
  char *s = hir_type_str(type);
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
  case HIR_EXPR_BUILTIN:
    return hir_tree_str_expr_builtin(buffer, pad, (hir_expr_builtin *)expr);
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

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_list_expr(string_buffer *buffer, size_t pad,
                                   list_hir_expr *list) {
  string_buffer_appendln(buffer, "[");
  pad += HIR_STR_TAB;
  for (list_hir_expr_it it = list_hir_expr_begin(list); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    hir_tree_str_expr(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "]");
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
  string_buffer_append(buffer, "args: ");
  hir_tree_str_list_expr(buffer, pad, expr->args);
  string_buffer_appendln(buffer, "");

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
  string_buffer_append(buffer, "args: ");
  hir_tree_str_list_expr(buffer, pad, expr->args);
  string_buffer_appendln(buffer, "");

  pad -= HIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void hir_tree_str_expr_builtin(string_buffer *buffer, size_t pad,
                                      hir_expr_builtin *expr) {
  string_buffer_appendln(buffer, "{");
  pad += HIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "base: ");
  hir_tree_str_expr_base(buffer, pad, &expr->base);
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "kind: ");
  string_buffer_append(buffer, hir_expr_builtin_enum_str(expr->kind));
  string_buffer_appendln(buffer, "");

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "args: ");
  hir_tree_str_list_expr(buffer, pad, expr->args);
  string_buffer_appendln(buffer, "");

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
    return;
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
  if (!stmt) {
    string_buffer_append(buffer, "(nil)");
    return;
  }

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
  if (subroutine->vars) {
    string_buffer_appendln(buffer, "vars: [");
    pad += HIR_STR_TAB;
    for (list_hir_var_it it = list_hir_var_begin(subroutine->vars);
         !it.end(&it); it.next(&it)) {
      string_buffer_append_padding(buffer, pad);
      hir_tree_str_var(buffer, pad, it.get(&it));
      string_buffer_appendln(buffer, "");
    }
    pad -= HIR_STR_TAB;
    string_buffer_append_padding(buffer, pad);
    string_buffer_appendln(buffer, "]");
  } else {
    string_buffer_appendln(buffer, "vars: (nil)");
  }

  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "body: ");
  hir_tree_str_stmt_block(buffer, pad, subroutine->body);
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
