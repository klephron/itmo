#include "node.h"

#include "compiler/hir/node_expr.h"
#include "compiler/hir/node_global.h"
#include "util/log.h"
#include "util/macro.h"
#include <string.h>

void hir_id_free(hir_id *id);
void hir_lit_free(hir_lit *self);
void hir_param_free(hir_param *self);
void hir_var_free(hir_var *self);

void hir_base_init(hir_base *self, span *span, hir_node_enum kind) {
  self->kind = kind;
  self->span = span;
}

void hir_base_deinit(hir_base *self) { span_free(self->span); }

void hir_node_free(hir_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case HIR_NODE_ID:
      return hir_id_free((hir_id *)generic);
    case HIR_NODE_LIT:
      return hir_lit_free((hir_lit *)generic);
    case HIR_NODE_PARAM:
      return hir_param_free((hir_param *)generic);
    case HIR_NODE_VAR:
      return hir_var_free((hir_var *)generic);
    case HIR_NODE_SUBROUTINE:
      return hir_subroutine_free((hir_subroutine *)generic);
    case HIR_NODE_EXPR:
      return hir_expr_free((hir_expr_base *)generic);
    case HIR_NODE_STMT:
      return hir_stmt_free((hir_stmt_base *)generic);
    case HIR_NODE_UNKNOWN:
      error("deleting unknown generic node");
      break;
    }
  }
}

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
hir_lit *hir_lit_new(span *span, type_base *type, hir_lit_u value) {
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
    case TYPE_STRING:
      u.v_str = strdup(self->value.v_str);
      break;
    case TYPE_CHAR:
      u.v_char = strdup(self->value.v_char);
      break;
    default:
      u = self->value;
      break;
    }
    return hir_lit_new(span_copy(self->base.span), type_copy(self->type), u);
  }
  return NULL;
}

void hir_lit_free(hir_lit *self) {
  if (self) {
    switch (self->type->type) {
    case TYPE_STRING:
    case TYPE_CHAR:
      free(self->value.v_str);
      break;
    default:
      break;
    }
    type_free(self->type);
    hir_base_deinit(&self->base);
    free(self);
  }
}
// HIR_PARAM
hir_param *hir_param_new(span *span, hir_id *id, type_base *type,
                         const hir_symbol *binding) {
  hir_param *self = MALLOC(hir_param);
  hir_base_init(&self->base, span, HIR_NODE_PARAM);
  self->id = id;
  self->type = type;
  self->binding = binding;
  return self;
}

void hir_param_free(hir_param *self) {
  if (self) {
    hir_id_free(self->id);
    type_free(self->type);
    hir_base_deinit(&self->base);
    free(self);
  }
}

hir_var *hir_var_new(span *span, hir_id *id, type_base *type,
                     const hir_symbol *binding) {
  hir_var *self = MALLOC(hir_var);
  hir_base_init(&self->base, span, HIR_NODE_VAR);
  self->id = id;
  self->type = type;
  self->binding = binding;
  return self;
}

void hir_var_free(hir_var *self) {
  if (self) {
    hir_id_free(self->id);
    type_free(self->type);
    hir_base_deinit(&self->base);
    free(self);
  }
}
