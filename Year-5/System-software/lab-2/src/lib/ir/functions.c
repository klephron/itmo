#include "ir/functions.h"

#include "util/macro.h"
#include <stdlib.h>
#include <string.h>

static void signature_free(ir_signature *data) {
  free(data->name);
  data->params->free(data->params);
  ir_t_free(data->ret);
}

void ir_subroutine_free(ir_subroutine *data) {
  if (data != NULL) {
    signature_free(data->signature);
    free(data->file_name);
    // TODO: free blocks (tree-like structure)
    data->operations->free(data->operations);
  }
}

ir_signature *ir_signature_new(char *name, p_list params, ir_t_base *ret) {
  ir_signature *self = MALLOC(ir_signature);
  self->name = name;
  self->params = params;
  self->ret = ret;
  return self;
}

ir_subroutine *ir_subroutine_new(const ir_userp *userp, ir_signature *signature,
                                 void *blocks, p_list operations) {
  ir_subroutine *self = MALLOC(ir_subroutine);
  self->signature = signature;
  self->file_name = (char *)userp->file_name;
  self->blocks = blocks;
  self->operations = operations;
  return self;
}

ir_subroutine_status_enum
ir_subroutine_status(const ir_subroutine *subroutine) {
  if (subroutine == NULL) {
    return IR_SUBROUTINE_STATUS_ABSENT;
  } else if (subroutine->blocks == NULL) { //  operations should be not NULL
    return IR_SUBROUTINE_STATUS_DECLARATION;
  } else {
    return IR_SUBROUTINE_STATUS_DEFINITION;
  }
}

int ir_subroutine_cmp(const ir_subroutine *lsv, const ir_subroutine *rsv) {
  if (lsv == NULL || rsv == NULL) {
    return IR_SUBROUTINE_CMP_NULL;
  }
  // compare signature
  ir_signature *lsv_s = lsv->signature;
  ir_signature *rsv_s = rsv->signature;

  int cmp = IR_SUBROUTINE_CMP_SAME;

  if (strcmp(lsv_s->name, rsv_s->name)) {
    cmp |= IR_SUBROUTINE_CMP_NAME;
  }

  if (!ir_t_equals(lsv_s->ret, lsv_s->ret)) {
    cmp |= IR_SUBROUTINE_CMP_RET;
  }

  int lsv_pc = lsv_s->params->size(lsv_s->params);
  int rsv_pc = rsv_s->params->size(rsv_s->params);

  if (lsv_pc != rsv_pc) {
    cmp |= IR_SUBROUTINE_CMP_PARAMS;
  } else {
    for (list_iter lpi = lsv_s->params->head, rpi = rsv_s->params->head;
         lpi != NULL; lpi = lpi->next, rpi = rpi->next) {

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
