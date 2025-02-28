#include "mir.h"
#include "util/log.h"
#include "util/macro.h"

mir_value *mir_value_new(size_t id, const symbol_entry *symbol_ref,
                         const type_entry *type_ref) {
  mir_value *self = MALLOC(mir_value);
  self->id = id;
  self->symbol_ref = symbol_ref;
  self->type_ref = type_ref;
  return self;
}

void mir_value_free(mir_value *self) {
  if (self) {
    free(self);
  }
}

mir_lit *mir_lit_new(size_t id, const type_entry *type_ref,
                     mir_lit_value value) {
  mir_lit *self = MALLOC(mir_lit);
  self->id = id;
  self->type_ref = type_ref;
  self->value = value;
  return self;
}

void mir_lit_free(mir_lit *self) {
  if (self) {
    type_enum kind = self->type_ref->type->kind;

    switch (kind) {
    case TYPE_PRIMITIVE: {
      type_primitive *primitive = (typeof(primitive))self;
      switch (primitive->type) {
      case TYPE_PRIMITIVE_STRING:
        if (self->value.v_str) {
          free(self->value.v_str);
        }
        break;
      case TYPE_PRIMITIVE_UNKNOWN:
      case TYPE_PRIMITIVE_BOOL:
      case TYPE_PRIMITIVE_BYTE:
      case TYPE_PRIMITIVE_INT:
      case TYPE_PRIMITIVE_UINT:
      case TYPE_PRIMITIVE_LONG:
      case TYPE_PRIMITIVE_ULONG:
      case TYPE_PRIMITIVE_CHAR:
      case TYPE_PRIMITIVE_VOID:
        break;
      }
      break;
    }
    case TYPE_ARRAY:
    case TYPE_CALLABLE:
    case TYPE_CLASS:
    case TYPE_TYPENAME:
    case TYPE_MONO:
    case TYPE_UNKNOWN:
      error("unsupported literal kind %d %p", kind, self);
      break;
    }
    free(self);
  }
}

mir_stmt *mir_stmt_new_op(mir_stmt_op_enum kind, mir_value *ret,
                          list_mir_value_ref *args) {
  mir_stmt *self = MALLOC(mir_stmt);
  self->kind = MIR_STMT_OP;
  self->op.kind = kind;
  self->op.ret = ret;
  self->op.args = args;
  return self;
}

mir_stmt *mir_stmt_new_call(mir_value *ret, mir_subroutine *sub,
                            list_mir_value_ref *args) {
  mir_stmt *self = MALLOC(mir_stmt);
  self->kind = MIR_STMT_CALL;
  self->call.ret = ret;
  self->call.sub = sub;
  self->call.args = args;
  return self;
}

mir_stmt *mir_stmt_new_member(mir_value *ret, mir_value *obj, mir_lit *member) {
  mir_stmt *self = MALLOC(mir_stmt);
  self->kind = MIR_STMT_MEMBER;
  self->member.ret = ret;
  self->member.obj = obj;
  self->member.member = member;
  return self;
}

mir_stmt *mir_stmt_new_builtin(mir_stmt_builtin_enum kind, mir_value *ret,
                               type_entry *type, list_mir_value_ref *args) {
  mir_stmt *self = MALLOC(mir_stmt);
  self->kind = MIR_STMT_BUILTIN;
  self->builtin.kind = kind;
  self->builtin.ret = ret;
  self->builtin.type = type;
  self->builtin.args = args;
  return self;
}

void mir_stmt_free(mir_stmt *self) {
  if (self) {
    switch (self->kind) {
    case MIR_STMT_OP:
      list_mir_value_ref_free(self->op.args);
      break;
    case MIR_STMT_CALL:
      list_mir_value_ref_free(self->call.args);
      break;
    case MIR_STMT_MEMBER:
      break;
    case MIR_STMT_BUILTIN:
      list_mir_value_ref_free(self->builtin.args);
      break;
    case MIR_STMT_UNKNOWN:
    default:
      error("unknown stmt kind %d %p", self->kind, self);
      break;
    }
    free(self);
  }
}

mir_bb *mir_bb_new(size_t id, list_mir_stmt *stmts, mir_value *jmp_cond_ref,
                   mir_bb *jmp_je_ref, mir_bb *jmp_jz_ref,
                   list_hir_expr_ref *hir_exprs) {
  mir_bb *self = MALLOC(mir_bb);
  self->id = id;
  self->stmts = stmts;
  self->jmp.cond_ref = jmp_cond_ref;
  self->jmp.je_ref = jmp_je_ref;
  self->jmp.jz_ref = jmp_jz_ref;
  self->hir_exprs = hir_exprs;
  return self;
}

void mir_bb_free(mir_bb *self) {
  if (self) {
    list_mir_stmt_free(self->stmts);
    list_hir_expr_ref_free(self->hir_exprs);
  }
}

mir_bb_enum mir_bb_get_cond(const mir_bb *self) {
  if (!self) {
    return MIR_BB_UNKNOWN;
  }
  if (self->jmp.cond_ref) {
    return MIR_BB_COND;
  }
  if (self->jmp.next_ref) {
    return MIR_BB_NEXT;
  }
  return MIR_BB_TERM;
}

mir_subroutine *mir_subroutine_new_basic(const symbol_entry *symbol_ref,
                                         const type_entry   *type_ref,
                                         mir_value *ret, list_mir_value *params,
                                         list_mir_value *vars,
                                         list_mir_value *tmps) {
  mir_subroutine *self = MALLOC(mir_subroutine);
  self->kind = MIR_SUBROUTINE_BASIC;
  self->symbol_ref = symbol_ref;
  self->type_ref = type_ref;
  self->basic.ret = ret;
  self->basic.params = params;
  self->basic.vars = vars;
  self->basic.tmps = tmps;
  return self;
}

mir_subroutine *mir_subroutine_new_external(const symbol_entry *symbol_ref,
                                            const type_entry   *type_ref,
                                            char               *name) {
  mir_subroutine *self = MALLOC(mir_subroutine);
  self->kind = MIR_SUBROUTINE_EXTERNAL;
  self->symbol_ref = symbol_ref;
  self->type_ref = type_ref;
  self->external.name = name;
  return self;
}

mir_subroutine *mir_subroutine_new_imported(const symbol_entry *symbol_ref,
                                            const type_entry   *type_ref,
                                            char *lib, char *entry) {
  mir_subroutine *self = MALLOC(mir_subroutine);
  self->kind = MIR_SUBROUTINE_IMPORTED;
  self->symbol_ref = symbol_ref;
  self->type_ref = type_ref;
  self->imported.lib = lib;
  self->imported.entry = entry;
  return self;
}

void mir_subroutine_free(mir_subroutine *self) {
  if (self) {
    switch (self->kind) {
    case MIR_SUBROUTINE_BASIC:
      mir_value_free(self->basic.ret);
      list_mir_value_free(self->basic.params);
      list_mir_value_free(self->basic.vars);
      list_mir_value_free(self->basic.tmps);
      break;
    case MIR_SUBROUTINE_EXTERNAL:
      if (self->external.name) {
        free(self->external.name);
      }
      break;
    case MIR_SUBROUTINE_IMPORTED:
      if (self->imported.lib) {
        free(self->imported.lib);
      }
      if (self->imported.entry) {
        free(self->imported.entry);
      }
      break;
    case MIR_SUBROUTINE_UNKNOWN:
      error("unknown subroutine kind %d %p", self->kind, self);
      break;
    }
    free(self);
  }
}

mir_class *mir_class_new(const type_entry *type_ref, list_mir_value *fields,
                         list_mir_subroutine_ref *methods) {
  mir_class *self = MALLOC(mir_class);
  self->type_ref = type_ref;
  self->fields = fields;
  self->methods = methods;
  return self;
}

void mir_class_free(mir_class *self) {
  if (self) {
    list_mir_value_free(self->fields);
    list_mir_subroutine_ref_free(self->methods);
    free(self);
  }
}

mir *mir_new() {
  mir *self = MALLOC(mir);
  self->global_subs = list_mir_subroutine_new();
  self->external_subs = list_mir_subroutine_new();
  self->imported_subs = list_mir_subroutine_new();
  self->methods = list_mir_subroutine_new();
  self->classes = list_mir_class_new();
  return self;
}

void mir_free(mir *self) {
  if (self) {
    list_mir_subroutine_free(self->global_subs);
    list_mir_subroutine_free(self->external_subs);
    list_mir_subroutine_free(self->imported_subs);
    list_mir_subroutine_free(self->methods);
    list_mir_class_free(self->classes);
    free(self);
  }
}
