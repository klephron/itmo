#include "hir.h"
#include "util/log.h"
#include "util/macro.h"

void hir_base_init(hir_base *self, span *span, hir_node_enum kind) {
  self->kind = kind;
  self->span = span;
}

void hir_base_deinit(hir_base *self) { span_free(self->span); }

// LIT
hir_lit *hir_lit_new(span *span, type_base *type, hir_lit_u value) {
  hir_lit *self = MALLOC(hir_lit);
  hir_base_init(&self->base, span, HIR_NODE_LIT);
  self->type = type;
  self->value = value;
  return self;
}

void hir_lit_free(hir_lit *self) {
  if (self) {
    switch (self->type->type) {
    case TYPE_STRING:
    case TYPE_CHAR:
      free(self->value.v_str);
      break;
    default:
      hir_node_free(self->value.v_node);
      break;
    }
    type_free(self->type);
    free(self);
  }
}

// SUBROUTINE
hir_subroutine *hir_subroutine_new(span *span, const hir_symbol *symbol) {
  hir_subroutine *self = MALLOC(hir_subroutine);
  hir_base_init(&self->base, span, HIR_NODE_SUBROUTINE);
  self->symbol = symbol;
  return self;
}
void hir_subroutine_free(hir_subroutine *self) {
  if (self) {
    hir_base_deinit(&self->base);
    free(self);
  }
}

// GENERIC
void hir_node_free(hir_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case HIR_NODE_LIT:
      return hir_lit_free((hir_lit *)generic);
    case HIR_NODE_SUBROUTINE:
      return hir_subroutine_free((hir_subroutine *)generic);
    default:
      error("deleting unknown generic node");
      break;
    }
  }
}

// HIR_ROOT
hir *hir_new() {
  hir *self = MALLOC(hir);
  self->symbols = list_hir_symbol_new();
  self->subroutines = list_hir_subroutine_new();
  return self;
}
void hir_free(hir *self) {
  if (self) {
    list_hir_symbol_free(self->symbols);
    self->symbols = NULL;
    list_hir_subroutine_free(self->subroutines);
    self->subroutines = NULL;
    free(self);
  }
}
