#include "pass1.h"
#include "exception.h"
#include "util/log.h"
#include "util/macro.h"

void ir_pass1_init(ir_pass1 *pass1, struct ir_ctx *ctx, const char *file_name) {
  pass1->base.kind = IR_PASS_1;
  pass1->ctx = ctx;
  pass1->file_name = file_name;
}

ir_subroutine *ir_pass1_subroutine_new(const ir_pass1 *pass1,
                                       ir_signature *signature) {
  return ir_subroutine_new(pass1->file_name, signature, NULL, NULL);
}

// TODO: maybe later allow overloading
int ir_pass1_subroutine_add(ir_pass1 *pass1, ir_subroutine *subroutine) {
  ir_ctx *ctx = pass1->ctx;

  for (list_iter it = ctx->subroutines->head; it != NULL; it = it->next) {
    ir_subroutine *lsv = it->data;
    int cmp = ir_subroutine_cmp(lsv, subroutine);
    int status = ir_subroutine_status(lsv);

    if ((cmp & IR_SUBROUTINE_CMP_ID) != 0) { // is names are different
      continue;
    }

    if (cmp != IR_SUBROUTINE_CMP_SAME) {
      exception *ex = exception_new_f(
          EXCEPTION_IR, EXCEPTION_IR_FUNCTION_OVERLOADING,
          subroutine->file_name, 0, 0, "function overloading is not allowed",
          "previous definition in %s %d:%d", lsv->file_name, 0, 0);
      ir_ctx_add_exception(ctx, ex);
      return 0;
    }

    if (status == IR_SUBROUTINE_STATUS_DEFINITION) {
      // TODO: may replace 0-s with actual location
      exception *ex = exception_new_f(
          EXCEPTION_IR, EXCEPTION_IR_FUNCTION_REDEFINITION,
          subroutine->file_name, 0, 0, "function redefinition",
          "previous definition in %s %d:%d", lsv->file_name, 0, 0);
      ir_ctx_add_exception(ctx, ex);
      return 0;
    }

    ctx->subroutines->replace(ctx->subroutines, it, subroutine);
    debug("replaced subroutine %s", subroutine->signature->id->name);
    return 1;
  }

  ctx->subroutines->put(ctx->subroutines, subroutine);
  debug("added subroutine %s", subroutine->signature->id->name);
  return 1;
}

int ir_pass1_param_add(ir_pass1 *pass1, p_list list, ir_param *param) {
  UNUSED(pass1);
  list->put(list, param);
  debug("param_list added param %s", param->id->name);
  return 1;
}
