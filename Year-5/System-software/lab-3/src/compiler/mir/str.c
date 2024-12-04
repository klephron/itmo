#include "str.h"
#include "compiler/hir/str.h"
#include "compiler/type/str.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/string_buffer.h"
#include <stdio.h>

#define MIR_STR_TAB 2
#define MIR_STR_SPACE " "

const char *mir_builtin_enum_str(mir_builtin_enum kind) {
  switch (kind) {
  case MIR_BUILTIN_STORE:
    return "STORE";
  case MIR_BUILTIN_PLUS:
    return "PLUS";
  case MIR_BUILTIN_MINUS:
    return "MINUS";
  case MIR_BUILTIN_LOGICAL_NOT:
    return "LOGICAL_NOT";
  case MIR_BUILTIN_BITWISE_NOT:
    return "BITWISE_NOT";
  case MIR_BUILTIN_INC:
    return "INC";
  case MIR_BUILTIN_DEC:
    return "DEC";
  case MIR_BUILTIN_LOGICAL_OR:
    return "LOGICAL_OR";
  case MIR_BUILTIN_LOGICAL_AND:
    return "LOGICAL_AND";
  case MIR_BUILTIN_BITWISE_OR:
    return "BITWISE_OR";
  case MIR_BUILTIN_BITWISE_XOR:
    return "BITWISE_XOR";
  case MIR_BUILTIN_BITWISE_AND:
    return "BITWISE_AND";
  case MIR_BUILTIN_EQUALS:
    return "EQUALS";
  case MIR_BUILTIN_NOT_EQUALS:
    return "NOT_EQUALS";
  case MIR_BUILTIN_LESS:
    return "LESS";
  case MIR_BUILTIN_LESS_EQUALS:
    return "LESS_EQUALS";
  case MIR_BUILTIN_GREATER:
    return "GREATER";
  case MIR_BUILTIN_GREATER_EQUALS:
    return "GREATER_EQUALS";
  case MIR_BUILTIN_BITWISE_SHIFT_LEFT:
    return "BITWISE_SHIFT_LEFT";
  case MIR_BUILTIN_BITWISE_SHIFT_RIGHT:
    return "BITWISE_SHIFT_RIGHT";
  case MIR_BUILTIN_ADD:
    return "ADD";
  case MIR_BUILTIN_SUB:
    return "SUB";
  case MIR_BUILTIN_MUL:
    return "MUL";
  case MIR_BUILTIN_DIV:
    return "DIV";
  case MIR_BUILTIN_MOD:
    return "MOD";
  case MIR_BUILTIN_CALL:
    return "CALL";
  case MIR_BUILTIN_INDEX:
    return "INDEX";
  case MIR_BUILTIN_CAST:
    return "CAST";
  case MIR_BUILTIN_NEW:
    return "NEW";
  case MIR_BUILTIN_DROP:
    return "DROP";
  default:
    info("unknown kind %d", kind);
    return "UNKNOWN";
  }
}

static void string_buffer_append_padding(string_buffer *buffer, size_t size) {
  for (size_t i = 0; i < size; ++i) {
    string_buffer_append(buffer, MIR_STR_SPACE);
  }
}

static void string_buffer_appendln(string_buffer *buffer, const char *data) {
  string_buffer_append(buffer, data);
  string_buffer_append(buffer, "\n");
}

static void mir_tree_str_type(string_buffer *buffer, size_t pad,
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

static void mir_tree_str_subroutine_ext(string_buffer *buffer, size_t pad,
                                        const mir_subroutine_ext *sub) {
  string_buffer_append(buffer, "method ");
  string_buffer_append(buffer, sub->name);
  string_buffer_append(buffer, "(");
  {
    list_type_it it = list_type_begin(sub->args);
    if (!it.end(&it)) {
      mir_tree_str_type(buffer, pad, it.get(&it));
      it.next(&it);
    }
    for (; !it.end(&it); it.next(&it)) {
      string_buffer_append(buffer, ", ");
      mir_tree_str_type(buffer, pad, it.get(&it));
    }
  }
  string_buffer_append(buffer, "): ");
  mir_tree_str_type(buffer, pad, sub->ret);
}

static void mir_tree_str_value(string_buffer *buffer, size_t pad,
                               const mir_value *value) {
  char buf[32];
  string_buffer_append(buffer, "VALUE(");
  snprintf(buf, STRMAXLEN(buf), "_%zu, ", value->id);
  string_buffer_append(buffer, buf);
  mir_tree_str_type(buffer, pad, value->type);
  string_buffer_append(buffer, ")");
}

static void mir_tree_str_stmt(string_buffer *buffer, size_t pad,
                              const mir_node_base *stmt) {
  char buf[32];

  if (!stmt) {
    return;
  }

  switch (stmt->kind) {
  case MIR_NODE_VALUE: {
    const mir_value *value = (mir_value *)stmt;
    snprintf(buf, STRMAXLEN(buf), "_%zu", value->id);
    string_buffer_append(buffer, buf);
  } break;
  case MIR_NODE_LIT: {
    const mir_lit *lit = (mir_lit *)stmt;
    char          *vs = hir_lit_value_str(lit->lit);
    char          *ts = type_str(lit->lit->type);

    if (ts) {
      string_buffer_append(buffer, "(");
      string_buffer_append(buffer, ts);
      string_buffer_append(buffer, ")");
      free(ts);
    }
    if (vs) {
      string_buffer_append(buffer, vs);
      free(vs);
    }
  } break;
  case MIR_NODE_BUILTIN: {
    const mir_builtin *builtin = (mir_builtin *)stmt;
    string_buffer_append(buffer, mir_builtin_enum_str(builtin->kind));
    if (builtin->spec) {
      string_buffer_append(buffer, "<");
      char *ts = type_str(builtin->spec);
      if (ts) {
        string_buffer_append(buffer, ts);
        free(ts);
      }
      string_buffer_append(buffer, ">");
    }

    string_buffer_append(buffer, "(");
    list_mir_arg_it it = list_mir_arg_begin(builtin->args);
    if (!it.end(&it)) {
      mir_tree_str_stmt(buffer, pad, it.get(&it));
      it.next(&it);
    }
    for (; !it.end(&it); it.next(&it)) {
      string_buffer_append(buffer, ", ");
      mir_tree_str_stmt(buffer, pad, it.get(&it));
    }
    string_buffer_append(buffer, ")");
  } break;
  case MIR_NODE_SUBROUTINE: {
    const mir_subroutine *sub = (mir_subroutine *)stmt;
    string_buffer_append(buffer, sub->name);
  } break;
  case MIR_NODE_SUBROUTINE_EXTERNAL: {
    const mir_subroutine_ext *sub = (mir_subroutine_ext *)stmt;
    string_buffer_append(buffer, sub->name);
  } break;
  case MIR_NODE_UNKNOWN:
  default:
    error("unexpected kind %d %p", stmt->kind, stmt);
  }
}

static void mir_tree_str_jmp(string_buffer *buffer, size_t pad,
                             const mir_jmp *jmp) {
  UNUSED(pad);

  char buf[32];

  if (jmp) {
    if (jmp->value_ref || jmp->je_ref) {
      string_buffer_append(buffer, "JE(");

      if (jmp->value_ref) {
        snprintf(buf, STRMAXLEN(buf), "_%zu, ", jmp->value_ref->id);
      } else {
        snprintf(buf, STRMAXLEN(buf), "(nil), ");
      }
      string_buffer_append(buffer, buf);

      if (jmp->je_ref) {
        snprintf(buf, STRMAXLEN(buf), "bb%zu, ", jmp->je_ref->id);
      } else {
        snprintf(buf, STRMAXLEN(buf), "(nil), ");
      }
      string_buffer_append(buffer, buf);

    } else {
      string_buffer_append(buffer, "JMP(");
    }

    if (jmp->jz_ref) {
      snprintf(buf, STRMAXLEN(buf), "bb%zu", jmp->jz_ref->id);
    } else {
      snprintf(buf, STRMAXLEN(buf), "(nil)");
    }
    string_buffer_append(buffer, buf);

    string_buffer_append(buffer, ")");
  } else {
    string_buffer_append(buffer, "return");
  }
}

static void mir_tree_str_bb(string_buffer *buffer, size_t pad,
                            const mir_bb *bb) {
  char buf[32];
  snprintf(buf, STRMAXLEN(buf), "bb%zu {", bb->id);
  string_buffer_appendln(buffer, buf);
  pad += MIR_STR_TAB;

  for (list_mir_node_it it = list_mir_node_begin(bb->stmts); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    mir_tree_str_stmt(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }

  string_buffer_append_padding(buffer, pad);
  mir_tree_str_jmp(buffer, pad, bb->jmp);
  string_buffer_appendln(buffer, "");

  pad -= MIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

static void mir_tree_str_subroutine(string_buffer *buffer, size_t pad,
                                    const mir_subroutine *sub) {
  string_buffer_append(buffer, "method ");
  string_buffer_append(buffer, sub->name);
  string_buffer_append(buffer, "(");
  {
    list_mir_value_it it = list_mir_value_begin(sub->args);
    if (!it.end(&it)) {
      mir_tree_str_type(buffer, pad, it.get(&it)->type);
      it.next(&it);
    }
    for (; !it.end(&it); it.next(&it)) {
      string_buffer_append(buffer, ", ");
      mir_tree_str_type(buffer, pad, it.get(&it)->type);
    }
  }
  string_buffer_append(buffer, "): ");
  mir_tree_str_type(buffer, pad, sub->ret->type);
  string_buffer_appendln(buffer, " {");
  pad += MIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  mir_tree_str_value(buffer, pad, sub->ret);
  string_buffer_appendln(buffer, "\t// ret");

  for (list_mir_value_it it = list_mir_value_begin(sub->args); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    mir_tree_str_value(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "\t// args");
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->vars); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    mir_tree_str_value(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "\t// vars");
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->tmps); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    mir_tree_str_value(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "\t// tmps");
  }

  for (list_mir_bb_it it = list_mir_bb_begin(sub->bbs); !it.end(&it);
       it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    mir_tree_str_bb(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }

  pad -= MIR_STR_TAB;
  string_buffer_append_padding(buffer, pad);
  string_buffer_append(buffer, "}");
}

char *mir_tree_str(const mir *mir) {
  string_buffer *buffer = string_buffer_new(0, 0);
  string_buffer_appendln(buffer, "{");
  size_t pad = MIR_STR_TAB;

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "EXTERNAL SUBROUTINES: [");
  pad += MIR_STR_TAB;
  for (list_mir_subroutine_ext_it it =
           list_mir_subroutine_ext_begin(mir->sub_exts);
       !it.end(&it); it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    mir_tree_str_subroutine_ext(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= MIR_STR_TAB;
  string_buffer_append_padding(buffer, MIR_STR_TAB);
  string_buffer_appendln(buffer, "]");

  string_buffer_append_padding(buffer, pad);
  string_buffer_appendln(buffer, "SUBROUTINES: [");
  pad += MIR_STR_TAB;
  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
       !it.end(&it); it.next(&it)) {
    string_buffer_append_padding(buffer, pad);
    mir_tree_str_subroutine(buffer, pad, it.get(&it));
    string_buffer_appendln(buffer, "");
  }
  pad -= MIR_STR_TAB;
  string_buffer_append_padding(buffer, MIR_STR_TAB);
  string_buffer_appendln(buffer, "]");

  string_buffer_append(buffer, "}");
  return string_buffer_detach(buffer);
}
