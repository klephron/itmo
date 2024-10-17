#include "pass2.h"

void ir_pass2_init(ir_pass2 *pass2, struct ir_ctx *ctx, const char *file_name) {
  pass2->base.kind = IR_PASS_2;
  pass2->ctx = ctx;
  pass2->file_name = file_name;
}
