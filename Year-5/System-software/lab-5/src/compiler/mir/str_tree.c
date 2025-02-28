#include "compiler/mir/mir.h"
#include "compiler/span/str.h"
#include "compiler/type_table/str.h"
#include "str.h"
#include "util/macro.h"
#include "util/strbuf.h"

static void strbuf_append_padding(strbuf *buffer, size_t size) {
  for (size_t i = 0; i < size; ++i) {
    strbuf_append(buffer, MIR_STR_SPACE);
  }
}

static void strbuf_appendln(strbuf *buffer) { strbuf_append(buffer, "\n"); }

static void strbuf_append_clx(strbuf *buffer, const char *data, size_t pad) {
  if (pad) {
    strbuf_appendln(buffer);
    strbuf_append_padding(buffer, pad);
  }
  strbuf_append(buffer, data);
}

static void mir_str_symbol(strbuf *buffer, size_t pad,
                           const symbol_entry *symbol) {
  UNUSED(pad);
  char buf[64];

  strbuf_append(buffer, symbol->name);

  strbuf_append_f(buffer, buf, ", symbol_table(%p)", symbol);
}

static void mir_str_type(strbuf *buffer, size_t pad, const type_entry *type) {
  UNUSED(pad);
  char buf[64];

  if (type) {
    char *type_s = type_str(type->type);
    if (type_s) {
      strbuf_append(buffer, type_s);
      free(type_s);
    } else {
      strbuf_append(buffer, "(nil)");
    }
    strbuf_append_f(buffer, buf, ", type_table(%p)", type);
  } else {
    strbuf_append(buffer, "any");
  }
}

static void mir_str_value(strbuf *buffer, size_t pad, const mir_value *value,
                          const char *prefix) {
  char buf[64];
  strbuf_append_clx(buffer, prefix, 0);
  strbuf_append(buffer, " ");
  strbuf_append_f(buffer, buf, "_%lu", value->id);
  strbuf_append(buffer, ": ");
  mir_str_type(buffer, pad, value->type_ref);
  if (value->symbol_ref) {
    strbuf_append(buffer, "  # ");
    mir_str_symbol(buffer, pad, value->symbol_ref);
  }
}

static void mir_str_stmt_args(strbuf *buffer, size_t pad,
                              const list_mir_value_ref *args) {
  UNUSED(pad);
  char buf[64];
  strbuf_append(buffer, "(");
  list_mir_value_ref_it it = list_mir_value_ref_begin(args);
  if (!END(it)) {
    strbuf_append_f(buffer, buf, "_%lu", GET(it)->id);
    for (NEXT(it); !END(it); NEXT(it)) {
      strbuf_append_f(buffer, buf, ", _%lu", GET(it)->id);
    }
  }
  strbuf_append(buffer, ")");
}

static void mir_str_stmt(strbuf *buffer, size_t pad, const mir_stmt *stmt) {
  UNUSED(pad);
  UNUSED(stmt);

  char buf[64];

  switch (stmt->kind) {

  case MIR_STMT_OP:
    if (stmt->op.ret) {
      strbuf_append_f(buffer, buf, "_%lu = ", stmt->op.ret->id);
    }
    strbuf_append(buffer, mir_stmt_enum_str(stmt->kind));
    strbuf_append(buffer, "_");
    strbuf_append(buffer, mir_stmt_op_enum_str(stmt->op.kind));
    mir_str_stmt_args(buffer, pad, stmt->op.args);
    break;

  case MIR_STMT_CALL:
    if (stmt->call.ret) {
      strbuf_append_f(buffer, buf, "_%lu = ", stmt->call.ret->id);
    }
    strbuf_append(buffer, stmt->call.sub->symbol_ref->name);
    mir_str_stmt_args(buffer, pad, stmt->op.args);
    break;

  case MIR_STMT_MEMBER:
    if (stmt->member.ret) {
      strbuf_append_f(buffer, buf, "_%lu = ", stmt->member.ret->id);
    }
    strbuf_append(buffer, mir_stmt_enum_str(stmt->kind));
    strbuf_append_f(buffer, buf, "(_%lu, ", stmt->member.obj->id);
    char *member_s = mir_lit_str(stmt->member.member);
    if (member_s) {
      strbuf_append(buffer, member_s);
      free(member_s);
    }
    strbuf_append(buffer, ")");
    break;

  case MIR_STMT_BUILTIN:
    if (stmt->builtin.ret) {
      strbuf_append_f(buffer, buf, "_%lu = ", stmt->builtin.ret->id);
    }
    strbuf_append(buffer, mir_stmt_enum_str(stmt->kind));
    strbuf_append(buffer, "_");
    strbuf_append(buffer, mir_stmt_builtin_enum_str(stmt->builtin.kind));
    if (stmt->builtin.type) {
      strbuf_append(buffer, "<");
      char *type_s = type_str(stmt->builtin.type->type);
      if (type_s) {
        strbuf_append(buffer, type_s);
        free(type_s);
      }
      strbuf_append(buffer, ">");
    }
    mir_str_stmt_args(buffer, pad, stmt->builtin.args);
    break;

  case MIR_STMT_UNKNOWN:
  default:
    warn("unexpected stmt kind %d %p", stmt->kind, stmt);
    break;
  }
}

static void mir_str_bb(strbuf *buffer, size_t pad, const mir_bb *bb) {
  char buf[64];
  strbuf_append_f(buffer, buf, "bb.%lu {", bb->id);
  pad += MIR_STR_TAB;

  strbuf_append_clx(buffer, "", pad);
  strbuf_append_f(buffer, buf, "hir_expr_trees_sz: %lu",
                  list_hir_expr_ref_size(bb->hir_exprs));

  if (!list_mir_stmt_empty(bb->stmts)) {
    strbuf_append_clx(buffer, "", pad);
    for (list_mir_stmt_it it = list_mir_stmt_begin(bb->stmts); !END(it);
         NEXT(it)) {
      const mir_stmt *stmt = GET(it);
      strbuf_append_clx(buffer, "", pad);
      mir_str_stmt(buffer, pad, stmt);
    }
  }

  strbuf_append_clx(buffer, "", pad);
  strbuf_append_clx(buffer, "", pad);
  mir_bb_enum bb_cond = mir_bb_get_cond(bb);
  switch (bb_cond) {
  case MIR_BB_COND:
    strbuf_append_f(buffer, buf, "cond(_%lu)", bb->jmp.cond_ref->id);
    strbuf_append(buffer, " -> [");
    strbuf_append_f(buffer, buf, "je: bb.%lu", bb->jmp.je_ref->id);
    strbuf_append(buffer, ", ");
    strbuf_append_f(buffer, buf, "jz: bb.%lu", bb->jmp.jz_ref->id);
    strbuf_append(buffer, "]");
    break;
  case MIR_BB_NEXT:
    strbuf_append_f(buffer, buf, "jmp() -> bb.%lu", bb->jmp.next_ref->id);
    break;
  case MIR_BB_TERM:
    strbuf_append(buffer, "return");
    break;
  case MIR_BB_UNKNOWN:
  default:
    warn("unexpected bb kind %d %p", bb_cond, bb);
    break;
  }

  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "}", pad);
}

static void mir_str_subroutine(strbuf *buffer, size_t pad,
                               const mir_subroutine *sub) {
  if (!sub) {
    strbuf_append_clx(buffer, "(nil)", 0);
    return;
  }

  strbuf_append(buffer, sub->symbol_ref->name);
  strbuf_append(buffer, " {");

  char *span_s = span_str(sub->symbol_ref->span);
  if (span_s) {
    strbuf_append(buffer, "  # ");
    strbuf_append(buffer, span_s);
    free(span_s);
  }

  pad += MIR_STR_TAB;

  strbuf_append_clx(buffer, "symbol: ", pad);
  mir_str_symbol(buffer, pad, sub->symbol_ref);

  strbuf_append_clx(buffer, "type_ref: ", pad);
  mir_str_type(buffer, pad, sub->type_ref);

  switch (sub->kind) {
  case MIR_SUBROUTINE_BASIC:
    strbuf_append_clx(buffer, "", pad);
    // ret
    strbuf_append_clx(buffer, "", pad);
    mir_str_value(buffer, pad, sub->basic.ret, "ret");
    // params
    for (list_mir_value_it it = list_mir_value_begin(sub->basic.params);
         !END(it); NEXT(it)) {
      const mir_value *param = GET(it);
      strbuf_append_clx(buffer, "", pad);
      mir_str_value(buffer, pad, param, "param");
    }
    // vars
    for (list_mir_value_it it = list_mir_value_begin(sub->basic.vars); !END(it);
         NEXT(it)) {
      const mir_value *param = GET(it);
      strbuf_append_clx(buffer, "", pad);
      mir_str_value(buffer, pad, param, "var");
    }
    // tmps
    for (list_mir_value_it it = list_mir_value_begin(sub->basic.tmps); !END(it);
         NEXT(it)) {
      const mir_value *param = GET(it);
      strbuf_append_clx(buffer, "", pad);
      mir_str_value(buffer, pad, param, "tmp");
    }
    // bbs
    strbuf_append_clx(buffer, "", pad);
    for (list_mir_bb_it it = list_mir_bb_begin(sub->basic.bbs); !END(it);
         NEXT(it)) {
      const mir_bb *bb = GET(it);
      strbuf_append_clx(buffer, "", pad);
      mir_str_bb(buffer, pad, bb);
    }
    break;
  case MIR_SUBROUTINE_EXTERNAL:
    break;
  case MIR_SUBROUTINE_IMPORTED:
    break;
  default:
    warn("unexpected subroutine kind %d %p", sub->kind, sub);
    break;
  }

  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "}", pad);
}

static void mir_str_class(strbuf *buffer, size_t pad, const mir_class *class) {
  if (!class) {
    strbuf_append_clx(buffer, "(nil)", 0);
    return;
  }

  strbuf_append_clx(buffer, "{", 0);
  pad += MIR_STR_TAB;

  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "}", pad);
}

char *mir_str(const mir *mir) {
  strbuf *buffer = strbuf_new(0, 0);
  size_t  pad = 0;

  strbuf_append_clx(buffer, "{", pad);
  pad += MIR_STR_TAB;

  // global subroutines
  strbuf_append_clx(buffer, "GLOBAL SUBROUTINES: [", pad);
  pad += MIR_STR_TAB;
  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->global_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *subroutine = GET(it);
    strbuf_append_clx(buffer, "", pad);
    mir_str_subroutine(buffer, pad, subroutine);
  }
  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "]", pad);

  // external subroutines
  strbuf_append_clx(buffer, "EXTERNAL SUBROUTINES: [", pad);
  pad += MIR_STR_TAB;
  for (list_mir_subroutine_it it =
           list_mir_subroutine_begin(mir->external_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *subroutine = GET(it);
    strbuf_append_clx(buffer, "", pad);
    mir_str_subroutine(buffer, pad, subroutine);
  }
  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "]", pad);

  // imported subroutines
  strbuf_append_clx(buffer, "IMPORTED SUBROUTINES: [", pad);
  pad += MIR_STR_TAB;
  for (list_mir_subroutine_it it =
           list_mir_subroutine_begin(mir->imported_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *subroutine = GET(it);
    strbuf_append_clx(buffer, "", pad);
    mir_str_subroutine(buffer, pad, subroutine);
  }
  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "]", pad);

  // methods
  strbuf_append_clx(buffer, "METHODS: [", pad);
  pad += MIR_STR_TAB;

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->methods);
       !END(it); NEXT(it)) {
    const mir_subroutine *subroutine = GET(it);
    strbuf_append_clx(buffer, "", pad);
    mir_str_subroutine(buffer, pad, subroutine);
  }
  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "]", pad);

  // classes
  strbuf_append_clx(buffer, "CLASSES: [", pad);
  pad += MIR_STR_TAB;

  for (list_mir_class_it it = list_mir_class_begin(mir->classes); !END(it);
       NEXT(it)) {
    const mir_class *class = GET(it);
    strbuf_append_clx(buffer, "", pad);
    mir_str_class(buffer, pad, class);
  }
  pad -= MIR_STR_TAB;
  strbuf_append_clx(buffer, "]", pad);

  pad -= MIR_STR_TAB;
  strbuf_appendln(buffer);
  strbuf_append_clx(buffer, "}", pad);

  return strbuf_detach(buffer);
}
