#include "ir/terminals.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void ir_term_base_free_in(ir_term_base *base) { UNUSED(base); }

static void ir_term_base_free(ir_term_base *base) {
  if (base != NULL) {
    ir_term_base_free_in(base);
    free(base);
  }
}

static void ir_term_base_init(ir_term_base *base, ir_term_enum kind) {
  base->kind = kind;
}

// LIT
void ir_term_lit_free(ir_term_lit *lit) {
  debug("lit free %p %d", lit, lit->type);
  if (lit != NULL) {
    if (lit->type == IR_T_STRING && lit->u.v_str) {
      free(lit->u.v_str);
    }
    ir_term_base_free_in(&lit->base);
    free(lit);
  }
}

// ID
void ir_term_id_free(ir_term_id *id) {
  debug("id free %p %s", id, id->name);
  if (id != NULL) {
    if (id->name) {
      free(id->name);
    }
    ir_term_base_free_in(&id->base);
    free(id);
  }
}

ir_term_id *ir_term_id_new(const char *str) {
  ir_term_id *id = MALLOC(ir_term_id);
  ir_term_base_init(&id->base, IR_TERM_IDENTIFIER);
  id->name = strdup(str);
  debug("created term_id %p %s", id, id->name);
  return id;
}

ir_t_custom *ir_term_id_to_t_custom(ir_term_id *id) {
  ir_t_custom *custom = ir_t_custom_new(id->name);
  ir_term_base_free_in(&id->base);
  free(id);
  return custom;
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
