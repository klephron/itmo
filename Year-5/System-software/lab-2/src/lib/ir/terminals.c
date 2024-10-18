#include "ir/terminals.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdlib.h>
#include <string.h>

static void ir_term_base_free_in(ir_term_base *self) { UNUSED(self); }

static void ir_term_base_free(ir_term_base *self) {
  if (self) {
    ir_term_base_free_in(self);
    free(self);
  }
}

static void ir_term_base_init(ir_term_base *self, ir_term_enum kind) {
  self->kind = kind;
}

// LIT
void ir_term_lit_free(ir_term_lit *self) {
  debug("lit free %p %d", self, self->type);
  if (self) {
    if (self->type == IR_T_STRING && self->u.v_str) {
      free(self->u.v_str);
    }
    ir_term_base_free_in(&self->base);
    free(self);
  }
}

// ID
void ir_term_id_free(ir_term_id *self) {
  debug("id free %p %s", self, self->name);
  if (self) {
    if (self->name) {
      free(self->name);
    }
    ir_term_base_free_in(&self->base);
    free(self);
  }
}

ir_term_id *ir_term_id_new(const char *str) {
  ir_term_id *self = MALLOC(ir_term_id);
  ir_term_base_init(&self->base, IR_TERM_IDENTIFIER);
  self->name = strdup(str);
  debug("created term_id %p %s", self, self->name);
  return self;
}

int ir_term_id_equals(const ir_term_id *lsv, const ir_term_id *rsv) {
  return !strcmp(lsv->name, rsv->name);
}

// GENERIC
void ir_term_free(struct ir_term_base *generic) {
  debug("generic free %p", generic);
  switch (generic->kind) {
  case IR_TERM_IDENTIFIER:
    return ir_term_id_free((ir_term_id *)generic);
  case IR_TERM_LITERAL:
    return ir_term_lit_free((ir_term_lit *)generic);
  default:
    return ir_term_base_free(generic);
  }
}
