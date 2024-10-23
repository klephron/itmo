#include "ir/subroutine.h"

#include "ir/terminal.h"
#include "util/container_util.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdlib.h>
#include <string.h>

// PARAM
void ir_var_free(ir_var *self) {
  // debug("before var free %p", self);
  if (self) {
    ir_term_free((ir_term_base *)self->id);
    ir_t_free(self->type);
    free(self);
  }
}

ir_var *ir_var_new(ir_term_id *id, ir_t_base *type) {
  ir_var *self = MALLOC(ir_var);
  self->type = type;
  self->id = id;
  /*debug("created var %s", id->name);*/
  return self;
}

const char *ir_var_name(const ir_var *self) {
  if (self && self->id) {
    return self->id->name;
  }
  return "<null>";
}

int ir_var_cmp(const ir_var *lsv, const ir_var *rsv) {
  return !ir_term_id_equals(lsv->id, rsv->id);
}

static int container_cmp_ir_var(const void *lsv, const void *rsv) {
  return ir_var_cmp(lsv, rsv);
}

static void container_delete_ir_var(void *data) {
  ir_var *self = data;
  ir_var_free(self);
}

p_list ir_var_list_new() {
  p_list list = list_new(container_cmp_ir_var, container_new_move,
                         container_delete_ir_var);
  return list;
}

static size_t container_hash_vars(const void *_data) {
  const ir_var *var = _data;
  return container_hash_chars(var->id->name);
}

p_hashset ir_var_hashset_new() {
  return hashset_new(container_cmp_ir_var, container_new_move,
                     container_delete_ir_var, container_hash_vars);
}

// SIGNATURE
static void signature_free(ir_signature *self) {
  if (self) {
    ir_term_free((ir_term_base *)self->id);
    list_free(self->params);
    ir_t_free(self->ret);
    free(self);
  }
}

ir_signature *ir_signature_new(ir_term_id *id, p_list params, ir_t_base *ret) {
  ir_signature *self = MALLOC(ir_signature);
  self->id = id;
  self->params = params;
  self->ret = ret;
  // debug("created signature %s with params=%zu and ret=%s", self->id->name,
  //       list_size(self->params), ir_t_str(self->ret));
  return self;
}

// SUBROUTINE
void ir_subroutine_free(ir_subroutine *self) {
  if (self) {
    signature_free(self->signature);
    if (self->file_name) {
      free(self->file_name);
    }
    ir_block_graph_free(self->cfg);
    hashset_free(self->vars);
    free(self);
  }
}

ir_subroutine *ir_subroutine_new(const char *file_name, ir_signature *signature,
                                 void *blocks, p_hashset vars) {
  ir_subroutine *self = MALLOC(ir_subroutine);
  self->signature = signature;
  self->file_name = file_name ? strdup((char *)file_name) : NULL;
  self->cfg = blocks;
  self->vars = vars;
  return self;
}

ir_subroutine_status_enum ir_subroutine_status(const ir_subroutine *self) {
  if (!self) {
    return IR_SUBROUTINE_STATUS_ABSENT;
  } else if (!self->cfg) {
    return IR_SUBROUTINE_STATUS_DECLARATION;
  } else {
    return IR_SUBROUTINE_STATUS_DEFINITION;
  }
}

const char *ir_subroutine_name(const ir_subroutine *self) {
  if (self && self->signature && self->signature->id) {
    return self->signature->id->name;
  }
  return "<null>";
}

int ir_subroutine_cmp(const ir_subroutine *lsv, const ir_subroutine *rsv) {
  if (!lsv || !rsv) {
    return IR_SUBROUTINE_CMP_NULL;
  }
  // compare signature
  ir_signature *lsv_s = lsv->signature;
  ir_signature *rsv_s = rsv->signature;

  int cmp = IR_SUBROUTINE_CMP_SAME;

  if (!ir_term_id_equals(lsv_s->id, rsv_s->id)) {
    cmp |= IR_SUBROUTINE_CMP_NAME;
  }

  if (!ir_t_equals(lsv_s->ret, rsv_s->ret)) {
    cmp |= IR_SUBROUTINE_CMP_RET;
  }

  // don't check params if NULL
  if (lsv_s->params && rsv_s->params) {
    int lsv_pc = list_size(lsv_s->params);
    int rsv_pc = list_size(rsv_s->params);

    if (lsv_pc != rsv_pc) {
      cmp |= IR_SUBROUTINE_CMP_PARAMS;
    } else {
      for (list_it lpi = lsv_s->params->head, rpi = rsv_s->params->head; lpi;
           lpi = lpi->next, rpi = rpi->next) {

        ir_var *lp = lpi->data;
        ir_var *rp = rpi->data;

        if (!ir_t_equals(lp->type, rp->type)) {
          cmp |= IR_SUBROUTINE_CMP_PARAMS;
          break;
        }
      }
    }
  }

  return cmp;
}
