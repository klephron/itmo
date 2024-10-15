#include "option2_inter/functions.h"

#include <stdlib.h>
#include "util/macro.h"

static void signature_free(option2_inter_signature *data) {
  free(data->name);
  data->params->free(data->params);
  option2_inter_t_free(data->ret);
}

void option2_inter_subroutine_free(option2_inter_subroutine *data) {
  if (data != NULL) {
    signature_free(data->signature);
    free(data->file_name);
    // TODO: free blocks (tree-like structure)
    data->operations->free(data->operations);
  }
}

option2_inter_signature *
option2_inter_signature_new(char *name, p_list params,
                            option2_inter_t_base *ret) {
  option2_inter_signature *self = MALLOC(option2_inter_signature);
  self->name = name;
  self->params = params;
  self->ret = ret;
  return self;
}

option2_inter_subroutine *
option2_inter_subroutine_new(const option2_inter_userp *userp,
                             option2_inter_signature *signature, void *blocks,
                             p_list operations) {
  option2_inter_subroutine *self = MALLOC(option2_inter_subroutine);
  self->signature = signature;
  self->file_name = (char *)userp->file_name;
  self->blocks = blocks;
  self->operations = operations;
  return self;
}
