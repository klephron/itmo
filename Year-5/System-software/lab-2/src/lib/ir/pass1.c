#include "pass1.h"
#include "exception.h"
#include "util/log.h"
#include "util/macro.h"

void ir_pass1_init(ir_pass1 *self, struct ir_ctx *ctx, const char *file_name) {
  self->base.kind = IR_PASS_1;
  self->ctx = ctx;
  self->file_name = file_name;
}

ir_subroutine *ir_pass1_subroutine_new(const ir_pass1 *self,
                                       ir_signature *signature) {
  return ir_subroutine_new(self->file_name, signature, NULL, NULL);
}

// TODO: maybe later allow overloading
int ir_pass1_subroutine_add(ir_pass1 *self, ir_subroutine *sub) {
  ir_ctx *ctx = self->ctx;

  int ret = 0;

  for (list_it it = ctx->subroutines->head; it; it = it->next) {
    ir_subroutine *lsv = it->data;
    int cmp = ir_subroutine_cmp(lsv, sub);

    debug("compare subroutines: %s %s = %d", ir_subroutine_name(lsv),
          ir_subroutine_name(sub), cmp);

    if (cmp & IR_SUBROUTINE_CMP_ID) { // is names are different
      continue;
    }

    if (cmp != IR_SUBROUTINE_CMP_SAME) {
      exception *ex = exception_new_f(
          EXCEPTION_IR, EXCEPTION_IR_FUNCTION_OVERLOADING, sub->file_name, 0, 0,
          "function overloading is not allowed",
          "previous definition of %s in %s:%d:%d", ir_subroutine_name(sub),
          lsv->file_name, 0, 0);
      ir_ctx_add_exception(ctx, ex);
      ret = -1;
      break;
    }

    ctx->subroutines->insert(ctx->subroutines, it, sub);
    debug("replaced subroutine %s", ir_subroutine_name(sub));
    ret = 1;
    break;
  }

  if (ret == 0) {
    ctx->subroutines->put(ctx->subroutines, sub);
    ret = 1;
    debug("added subroutine %s", ir_subroutine_name(sub));
  } else if (ret == -1) {
    ir_subroutine_free(sub);
    ret = 0;
  }

  return ret;
}

int ir_pass1_param_add(ir_pass1 *self, p_list list, ir_param *param) {
  UNUSED(self);
  list->put(list, param);
  debug("param_list added param %s", ir_param_name(param));
  return 1;
}
