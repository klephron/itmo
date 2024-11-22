#include "eir.h"
#include "util/log.h"
#include "util/macro.h"

// EXPR
void eir_expr_base_init(eir_expr_base *self, span *span, eir_expr_enum kind,
                        type_base *type) {
  self->kind = kind;
  self->type = type;
  self->span = span;
}

void eir_expr_base_deinit(eir_expr_base *self) {
  type_free(self->type);
  span_free(self->span);
}

void eir_expr_free(eir_expr_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case EIR_EXPR_UNARY:
      return eir_expr_unary_free((eir_expr_unary *)generic);
    case EIR_EXPR_BINARY:
      return eir_expr_binary_free((eir_expr_binary *)generic);
    case EIR_EXPR_LITERAL:
      return eir_expr_lit_free((eir_expr_lit *)generic);
    case EIR_EXPR_IDENTIFIER:
      return eir_expr_id_free((eir_expr_id *)generic);
    case EIR_EXPR_CALL:
      return eir_expr_call_free((eir_expr_call *)generic);
    case EIR_EXPR_INDEX:
      return eir_expr_index_free((eir_expr_index *)generic);
    case EIR_EXPR_BUILTIN:
      return eir_expr_builtin_free((eir_expr_builtin *)generic);
    default:
      error("unknown eir_expr_enum %d %p", generic->kind, generic);
    }
  }
}

eir_expr_unary *eir_expr_unary_new(span *span, type_base *type,
                                   eir_expr_unary_enum op,
                                   eir_expr_base      *first) {
  eir_expr_unary *self = MALLOC(eir_expr_unary);
  eir_expr_base_init(&self->base, span, EIR_EXPR_UNARY, type);
  self->op = op;
  self->first = first;
  return self;
}

void eir_expr_unary_free(eir_expr_unary *self) {
  if (self) {
    eir_expr_free(self->first);
    eir_expr_base_deinit(&self->base);
    free(self);
  }
}

eir_expr_binary *eir_expr_binary_new(span *span, type_base *type,
                                     eir_expr_binary_enum op,
                                     eir_expr_base       *first,
                                     eir_expr_base       *second) {

  eir_expr_binary *self = MALLOC(eir_expr_binary);
  eir_expr_base_init(&self->base, span, EIR_EXPR_BINARY, type);
  self->op = op;
  self->first = first;
  self->second = second;
  return self;
}

void eir_expr_binary_free(eir_expr_binary *self) {
  if (self) {
    eir_expr_free(self->first);
    eir_expr_free(self->second);
    eir_expr_base_deinit(&self->base);
    free(self);
  }
}

eir_expr_lit *eir_expr_lit_new(span *span, type_base *type, hir_lit *lit) {
  eir_expr_lit *self = MALLOC(eir_expr_lit);
  eir_expr_base_init(&self->base, span, EIR_EXPR_LITERAL, type);
  self->lit = lit;
  return self;
}

void eir_expr_lit_free(eir_expr_lit *self) {
  if (self) {
    hir_lit_free(self->lit);
    eir_expr_base_deinit(&self->base);
    free(self);
  }
}

eir_expr_id *eir_expr_id_new(span *span, type_base *type,
                             const hir_symbol *binding) {
  eir_expr_id *self = MALLOC(eir_expr_id);
  eir_expr_base_init(&self->base, span, EIR_EXPR_IDENTIFIER, type);
  self->binding = binding;
  return self;
}

void eir_expr_id_free(eir_expr_id *self) {
  if (self) {
    eir_expr_base_deinit(&self->base);
    free(self);
  }
}

eir_expr_call *eir_expr_call_new(span *span, type_base *type,
                                 eir_expr_base *callee, list_eir_expr *args) {
  eir_expr_call *self = MALLOC(eir_expr_call);
  eir_expr_base_init(&self->base, span, EIR_EXPR_CALL, type);
  self->callee = callee;
  self->args = args;
  return self;
}

void eir_expr_call_free(eir_expr_call *self) {
  if (self) {
    list_eir_expr_free(self->args);
    eir_expr_free(self->callee);
    eir_expr_base_deinit(&self->base);
    free(self);
  }
}

eir_expr_index *eir_expr_index_new(span *span, type_base *type,
                                   eir_expr_base *indexed,
                                   list_eir_expr *args) {
  eir_expr_index *self = MALLOC(eir_expr_index);
  eir_expr_base_init(&self->base, span, EIR_EXPR_INDEX, type);
  self->indexed = indexed;
  self->args = args;
  return self;
}

void eir_expr_index_free(eir_expr_index *self) {
  if (self) {
    list_eir_expr_free(self->args);
    eir_expr_free(self->indexed);
    eir_expr_base_deinit(&self->base);
    free(self);
  }
}

eir_expr_builtin *eir_expr_builtin_new(span *span, type_base *type,
                                       eir_expr_builtin_enum kind,
                                       list_eir_expr        *args) {
  eir_expr_builtin *self = MALLOC(eir_expr_builtin);
  eir_expr_base_init(&self->base, span, EIR_EXPR_BUILTIN, type);
  self->kind = kind;
  self->args = args;
  return self;
}

void eir_expr_builtin_free(eir_expr_builtin *self) {
  if (self) {
    list_eir_expr_free(self->args);
    eir_expr_base_deinit(&self->base);
    free(self);
  }
}

// BLOCK
void eir_block_base_init(eir_block_base *self, size_t id, span *span,
                         eir_block_enum kind) {
  self->id = id;
  self->kind = kind;
  self->span = span;
}

void eir_block_base_deinit(eir_block_base *self) { span_free(self->span); }

static inline eir_block_enum eir_block_kind(eir_block_base *base) {
  return base->kind & (~EIR_BLOCK_UNTIED);
}

static inline int eir_block_untied(eir_block_base *base) {
  return base->kind & EIR_BLOCK_UNTIED;
}

void eir_block_free(eir_block_base *generic) {
  if (generic) {
    eir_block_enum kind = eir_block_kind(generic);
    switch (kind) {
    case EIR_BLOCK_NEXT:
      return eir_block_next_free((eir_block_next *)generic);
    case EIR_BLOCK_COND:
      return eir_block_cond_free((eir_block_cond *)generic);
    default:
      error("unknown block kind %d %p", generic->kind, generic);
    }
  }
}

eir_block_next *eir_block_next_new(size_t id, span *span, eir_expr_base *expr,
                                   eir_block_base *next) {
  eir_block_next *self = MALLOC(eir_block_next);
  eir_block_base_init(&self->base, id, span, EIR_BLOCK_NEXT);
  self->expr = expr;
  self->next = next;
  return self;
}

void eir_block_next_free(eir_block_next *self) {
  if (self) {
    eir_expr_free(self->expr);
    eir_block_free(self->next);
    eir_block_base_deinit(&self->base);
    free(self);
  }
}

eir_block_cond *eir_block_cond_new(size_t id, span *span, eir_expr_base *cond,
                                   eir_block_base *je, eir_block_base *jz) {
  eir_block_cond *self = MALLOC(eir_block_cond);
  eir_block_base_init(&self->base, id, span, EIR_BLOCK_COND);
  self->cond = cond;
  self->jz = jz;
  self->je = je;
  return self;
}

void eir_block_cond_free(eir_block_cond *self) {
  if (self) {
    eir_expr_free(self->cond);
    eir_block_free(self->jz);
    eir_block_free(self->je);
    eir_block_base_deinit(&self->base);
    free(self);
  }
}

void eir_block_set_next(eir_block_base *self, eir_block_base *next) {
  if (self) {
    switch (self->kind) {
    case EIR_BLOCK_NEXT:
      ((eir_block_next *)self)->next = next;
      break;
    case EIR_BLOCK_COND:
      ((eir_block_cond *)self)->jz = next;
      break;
    default:
      error("unexpected block type");
    }
  }
}

eir_block_base *eir_block_get_next(eir_block_base *self) {
  if (self) {
    switch (self->kind) {
    case EIR_BLOCK_NEXT:
      return ((eir_block_next *)self)->next;
    case EIR_BLOCK_COND:
      return ((eir_block_cond *)self)->jz;
    default:
      error("unexpected block type");
    }
  }
  return NULL;
}

// SUBROUTINE
eir_subroutine *eir_subroutine_new(eir_block_base       *root,
                                   const hir_symbol     *binding,
                                   const list_hir_param *params_ref,
                                   const list_hir_var   *vars_ref) {
  eir_subroutine *self = MALLOC(eir_subroutine);
  self->root = root;
  self->binding = binding;
  self->params_ref = params_ref;
  self->vars_ref = vars_ref;
  return self;
}

static void eir_subroutine_graph_to_tree(eir_block_base *generic) {
  generic->kind |= EIR_BLOCK_UNTIED;

  switch (eir_block_kind(generic)) {
  case EIR_BLOCK_NEXT: {
    eir_block_next *cur = (eir_block_next *)generic;

    if (cur->next && !eir_block_untied(cur->next)) {
      eir_subroutine_graph_to_tree(cur->next);
    } else {
      cur->next = NULL;
    }
  } break;
  case EIR_BLOCK_COND: {
    eir_block_cond *cur = (eir_block_cond *)generic;

    if (cur->jz && !eir_block_untied(cur->jz)) {
      eir_subroutine_graph_to_tree(cur->jz);
    } else {
      cur->jz = NULL;
    }
    if (cur->je && !eir_block_untied(cur->je)) {
      eir_subroutine_graph_to_tree(cur->je);
    } else {
      cur->je = NULL;
    }
  } break;
  default:
    error("unexpected block type");
  }
}

void eir_subroutine_free(eir_subroutine *self) {
  if (self) {
    if (self->root) {
      eir_subroutine_graph_to_tree(self->root);
    }
    eir_block_free(self->root);
    self->binding = NULL;
    free(self);
  }
}

// EIR
eir *eir_new() {
  eir *self = MALLOC(eir);
  self->globals = NULL;
  self->locals = NULL;
  self->subroutines = list_eir_subroutine_new();
  return self;
}

void eir_free(eir *self) {
  if (self) {
    self->globals = NULL;
    self->locals = NULL;
    list_eir_subroutine_free(self->subroutines);
    self->subroutines = NULL;
    free(self);
  }
}
