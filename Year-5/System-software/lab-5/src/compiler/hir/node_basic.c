#include "node_basic.h"

#include "compiler/hir/node.h"
#include "compiler/span/span.h"
#include "util/macro.h"
#include <string.h>

// ID
hir_id *hir_id_new(span *span, char *name) {
  hir_id *self = MALLOC(hir_id);
  hir_base_init(&self->base, span, HIR_NODE_ID);
  self->name = name;
  return self;
}

hir_id_data hir_id_pop(hir_id *id) {
  hir_id_data data = {};
  if (id) {
    data.span = id->base.span;
    data.name = id->name;
    free(id);
  }
  return data;
}

hir_id *hir_id_copy(const hir_id *id) {
  return hir_id_new(span_copy(id->base.span), strdup(id->name));
}

void hir_id_free(hir_id *id) {
  if (id) {
    free(id->name);
    hir_base_deinit(&id->base);
    free(id);
  }
}

// LIT
hir_lit *hir_lit_new(span *span, hir_type_base *type, hir_lit_u value) {
  hir_lit *self = MALLOC(hir_lit);
  hir_base_init(&self->base, span, HIR_NODE_LIT);
  self->type = type;
  self->value = value;
  return self;
}

hir_lit *hir_lit_copy(const hir_lit *self) {
  if (self) {
    hir_lit_u u;
    switch (self->type->type) {
    case HIR_TYPE_STRING:
      u.v_str = strdup(self->value.v_str);
      break;
    case HIR_TYPE_CHAR:
      u.v_char = strdup(self->value.v_char);
      break;
    default:
      u = self->value;
      break;
    }
    return hir_lit_new(span_copy(self->base.span), hir_type_copy(self->type),
                       u);
  }
  return NULL;
}

void hir_lit_free(hir_lit *self) {
  if (self) {
    switch (self->type->type) {
    case HIR_TYPE_STRING:
    case HIR_TYPE_CHAR:
      free(self->value.v_str);
      break;
    default:
      break;
    }
    hir_type_free(self->type);
    hir_base_deinit(&self->base);
    free(self);
  }
}
// PARAM
hir_param *hir_param_new(span *span, hir_id *id, hir_type_base *type) {
  hir_param *self = MALLOC(hir_param);
  hir_base_init(&self->base, span, HIR_NODE_PARAM);
  self->id = id;
  self->type = type;
  return self;
}

void hir_param_free(hir_param *self) {
  if (self) {
    hir_id_free(self->id);
    hir_type_free(self->type);
    hir_base_deinit(&self->base);
    free(self);
  }
}

// VAR
hir_var *hir_var_new(span *span, hir_id *id, hir_type_base *type) {
  hir_var *self = MALLOC(hir_var);
  hir_base_init(&self->base, span, HIR_NODE_VAR);
  self->id = id;
  self->type = type;
  return self;
}

void hir_var_free(hir_var *self) {
  if (self) {
    hir_id_free(self->id);
    hir_type_free(self->type);
    hir_base_deinit(&self->base);
    free(self);
  }
}
