#include "mir.h"
#include "util/log.h"
#include "util/macro.h"

static inline void mir_node_init(mir_node_base *self, mir_node_enum kind) {
  self->kind = kind;
}

static inline void mir_node_deinit(mir_node_base *self) { UNUSED(self); }

void mir_node_free(mir_node_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case MIR_NODE_VALUE:
      return mir_value_free((mir_value *)generic);
    case MIR_NODE_LIT:
      return mir_lit_free((mir_lit *)generic);
    case MIR_NODE_BUILTIN:
      return mir_builtin_free((mir_builtin *)generic);
    case MIR_NODE_SUBROUTINE:
      return mir_subroutine_free((mir_subroutine *)generic);
    case MIR_NODE_SUBROUTINE_EXTERNAL:
      return mir_subroutine_ext_free((mir_subroutine_ext *)generic);
    default:
      error("unknown mir node %d %p", generic->kind, generic);
    }
  }
}

mir_value *mir_value_new(size_t id, type_base *type) {
  mir_value *self = MALLOC(mir_value);
  mir_node_init(&self->base, MIR_NODE_VALUE);
  self->id = id;
  self->type = type;
  return self;
}

void mir_value_free(mir_value *self) {
  if (self) {
    type_free(self->type);
    mir_node_deinit(&self->base);
    free(self);
  }
}

mir_lit *mir_lit_new(hir_lit *lit) {
  mir_lit *self = MALLOC(mir_lit);
  mir_node_init(&self->base, MIR_NODE_LIT);
  self->lit = lit;
  return self;
}

void mir_lit_free(mir_lit *self) {
  if (self) {
    hir_lit_free(self->lit);
    mir_node_deinit(&self->base);
    free(self);
  }
}

mir_builtin *mir_builtin_new(mir_builtin_enum kind, list_mir_arg *args,
                             type_base *spec, mir_debug_line *debug_line) {
  mir_builtin *self = MALLOC(mir_builtin);
  mir_node_init(&self->base, MIR_NODE_BUILTIN);
  self->kind = kind;
  self->args = args;
  self->spec = spec;
  self->debug_line = debug_line;
  return self;
}

void mir_builtin_free(mir_builtin *self) {
  if (self) {
    list_mir_arg_free(self->args);
    type_free(self->spec);
    mir_debug_line_free(self->debug_line);
    mir_node_deinit(&self->base);
    free(self);
  }
}

mir_jmp *mir_jmp_new(mir_value *value_ref, mir_bb *je_ref, mir_bb *jz_ref,
                     mir_debug_line *debug_line) {
  mir_jmp *self = MALLOC(mir_jmp);
  self->value_ref = value_ref;
  self->je_ref = je_ref;
  self->jz_ref = jz_ref;
  self->debug_line = debug_line;
  return self;
}

void mir_jmp_free(mir_jmp *self) {
  if (self) {
    mir_debug_line_free(self->debug_line);
    free(self);
  }
}

mir_bb *mir_bb_new(size_t id, list_mir_node *stmts, mir_jmp *jmp) {
  mir_bb *self = MALLOC(mir_bb);
  self->id = id;
  self->stmts = stmts;
  self->jmp = jmp;
  return self;
}

void mir_bb_free(mir_bb *self) {
  if (self) {
    list_mir_node_free(self->stmts);
    mir_jmp_free(self->jmp);
    free(self);
  }
}

mir_subroutine *mir_subroutine_new(char *name, mir_value *ret,
                                   list_mir_value *args, list_mir_value *vars,
                                   list_mir_value *tmps, list_mir_bb *bbs) {
  mir_subroutine *self = MALLOC(mir_subroutine);
  mir_node_init(&self->base, MIR_NODE_SUBROUTINE);
  self->name = name;
  self->ret = ret;
  self->args = args;
  self->vars = vars;
  self->tmps = tmps;
  self->bbs = bbs;
  return self;
}

void mir_subroutine_free(mir_subroutine *self) {
  if (self) {
    free(self->name);
    list_mir_bb_free(self->bbs);
    mir_value_free(self->ret);
    list_mir_value_free(self->args);
    list_mir_value_free(self->vars);
    list_mir_value_free(self->tmps);
    free(self);
  }
}

mir_subroutine_ext *mir_subroutine_ext_new(char *name, type_base *ret,
                                           list_type *args) {
  mir_subroutine_ext *self = MALLOC(mir_subroutine_ext);
  mir_node_init(&self->base, MIR_NODE_SUBROUTINE_EXTERNAL);
  self->name = name;
  self->ret = ret;
  self->args = args;
  return self;
}

void mir_subroutine_ext_free(mir_subroutine_ext *self) {
  if (self) {
    free(self->name);
    type_free(self->ret);
    list_type_free(self->args);
    free(self);
  }
}

mir *mir_new() {
  mir *self = MALLOC(mir);
  self->subs = list_mir_subroutine_new();
  self->sub_exts = list_mir_subroutine_ext_new();
  return self;
}

void mir_free(mir *self) {
  if (self) {
    // delete blocks before, because it can reference globals
    if (self->subs) {
      for (list_mir_subroutine_it it = list_mir_subroutine_begin(self->subs);
           !it.end(&it); it.next(&it)) {
        mir_subroutine *sub = it.get(&it);
        list_mir_bb_free(sub->bbs);
        sub->bbs = NULL;
      }
    }
    list_mir_subroutine_free(self->subs);
    list_mir_subroutine_ext_free(self->sub_exts);
    free(self);
  }
}
