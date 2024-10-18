#include "ir/functions.h"

#include "ir/terminals.h"
#include "util/container_funcs.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdlib.h>
#include <string.h>

// PARAM
void ir_param_free(ir_param *self) {
  debug("before param free %p", self);
  if (self) {
    ir_term_free((ir_term_base *)self->id);
    ir_t_free(self->type);
    free(self);
  }
}

ir_param *ir_param_new(ir_term_id *id, ir_t_base *type) {
  ir_param *self = MALLOC(ir_param);
  self->type = type;
  self->id = id;
  debug("created param %s", id->name);
  return self;
}

const char *ir_param_name(const ir_param *self) {
  if (self && self->id) {
    return self->id->name;
  }
  return "<null>";
}

static int container_cmp_ir_param(const void *_lsv, const void *_rsv) {
  const ir_param *lsv = _lsv;
  const ir_param *rsv = _rsv;
  return !ir_term_id_equals(lsv->id, rsv->id);
}

static void container_delete_ir_param(void *data) {
  ir_param *self = data;
  ir_param_free(self);
}

p_list ir_param_list_new() {
  p_list list = list_new(container_cmp_ir_param, container_new_move,
                         container_delete_ir_param, 0);
  return list;
}

// SIGNATURE
static void signature_free(ir_signature *self) {
  if (self) {
    ir_term_free((ir_term_base *)self->id);
    self->params->free(self->params);
    ir_t_free(self->ret);
    free(self);
  }
}

ir_signature *ir_signature_new(ir_term_id *id, p_list params, ir_t_base *ret) {
  ir_signature *self = MALLOC(ir_signature);
  self->id = id;
  self->params = params;
  self->ret = ret;
  debug("created signature %s with params=%zu and ret=%s", self->id->name,
        self->params->size(self->params), ir_t_type_str(self->ret));
  return self;
}

// SUBROUTINE
void ir_subroutine_free(ir_subroutine *self) {
  if (self) {
    signature_free(self->signature);
    free(self->file_name);
    if (self->blocks) {
      // TODO: free data blocks
    }
    if (self->operations) {
      self->operations->free(self->operations);
    }
    free(self);
  }
}

ir_subroutine *ir_subroutine_new(const char *file_name, ir_signature *signature,
                                 void *blocks, p_list operations) {
  ir_subroutine *self = MALLOC(ir_subroutine);
  self->signature = signature;
  self->file_name = strdup((char *)file_name);
  self->blocks = blocks;
  self->operations = operations;
  debug("created subroutine %s file=%s", ir_subroutine_name(self), file_name);
  return self;
}

ir_subroutine_status_enum ir_subroutine_status(const ir_subroutine *self) {
  if (!self) {
    return IR_SUBROUTINE_STATUS_ABSENT;
  } else if (!self->blocks) {
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
    cmp |= IR_SUBROUTINE_CMP_ID;
  }

  if (!ir_t_equals(lsv_s->ret, rsv_s->ret)) {
    cmp |= IR_SUBROUTINE_CMP_RET;
  }

  int lsv_pc = lsv_s->params->size(lsv_s->params);
  int rsv_pc = rsv_s->params->size(rsv_s->params);

  if (lsv_pc != rsv_pc) {
    cmp |= IR_SUBROUTINE_CMP_PARAMS;
  } else {
    for (list_it lpi = lsv_s->params->head, rpi = rsv_s->params->head; lpi;
         lpi = lpi->next, rpi = rpi->next) {

      ir_param *lp = lpi->data;
      ir_param *rp = rpi->data;

      if (!ir_t_equals(lp->type, rp->type)) {
        cmp |= IR_SUBROUTINE_CMP_PARAMS;
        break;
      }
    }
  }

  return cmp;
}
