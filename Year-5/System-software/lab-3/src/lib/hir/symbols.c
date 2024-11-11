#include "symbols.h"
#include "util/macro.h"
#include <string.h>

hir_symbol *hir_symbol_new(char *name, hir_binding_enum bind_kind,
                           const hir_symbol *bind_parent, type_base *type,
                           span *decl_span) {
  hir_symbol *self = MALLOC(hir_symbol);
  self->name = name;
  self->bind_kind = bind_kind;
  self->bind_parent = bind_parent;
  self->type = type;
  self->decl_span = decl_span;
  return self;
}

void hir_symbol_free(hir_symbol *self) {
  if (self) {
    free(self->name);
    type_free(self->type);
    span_free(self->decl_span);
    free(self);
  }
}
