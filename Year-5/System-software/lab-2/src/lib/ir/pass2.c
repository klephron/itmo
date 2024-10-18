#include "pass2.h"

void ir_pass2_init(ir_pass2 *self, struct ir_ctx *ctx, const char *file_name) {
  self->base.kind = IR_PASS_2;
  self->ctx = ctx;
  self->file_name = file_name;
}

// int status = ir_subroutine_status(lsv);

// if (status == IR_SUBROUTINE_STATUS_DEFINITION) {
//   // TODO: may replace 0-s with actual location
//   exception *ex = exception_new_f(
//       EXCEPTION_IR, EXCEPTION_IR_FUNCTION_REDEFINITION,
//       subroutine->file_name, 0, 0, "function redefinition",
//       "previous definition in %s %d:%d", lsv->file_name, 0, 0);
//   ir_ctx_add_exception(ctx, ex);
//   ret = -1;
//   break;
// }
