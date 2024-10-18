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
                                       ir_signature   *signature) {
  return ir_subroutine_new(self->file_name, signature, NULL);
}

static int ir_pass1_subroutine_validate(ir_pass1 *self, ir_subroutine *sub) {
  p_list params = sub->signature->params;
  for (list_it it = params->head; it; it = it->next) {
    for (list_it it2 = it->next; it2; it2 = it2->next) {
      if (!ir_param_cmp(it->data, it2->data)) {
        exception *ex = exception_new_f(
            EXCEPTION_IR, EXCEPTION_IR_FUNCTION_PARAMETER_DUPLICATION,
            sub->file_name, 0, 0, "function parameter duplication",
            "function %s parameter %s is duplicated", ir_subroutine_name(sub),
            ir_param_name(it->data));
        ir_ctx_add_exception(self->ctx, ex);
        return 0;
      }
    }
  }
  return 1;
}

// TODO: maybe later allow overloading (will require multiset)
int ir_pass1_subroutine_add(ir_pass1 *self, ir_subroutine *sub) {
  int     ok = 1;
  ir_ctx *ctx = self->ctx;

  if (!ir_pass1_subroutine_validate(self, sub)) {
    ok = 0;
  }

  if (ok) {
    hashset_it it = hashset_find(ctx->subroutines, sub);

    if (it.end(&it)) {
      hashset_insert(ctx->subroutines, sub);
      debug("added subroutine %s", ir_subroutine_name(sub));
    } else {
      ir_subroutine *lsv = it.get(&it);
      int            cmp = ir_subroutine_cmp(lsv, sub);

      if (cmp == IR_SUBROUTINE_CMP_SAME) {
        hashset_insert(ctx->subroutines, sub);
        debug("replaced subroutine %s", ir_subroutine_name(sub));
      } else {
        exception *ex = exception_new_f(
            EXCEPTION_IR, EXCEPTION_IR_FUNCTION_OVERLOADING, sub->file_name, 0,
            0, "function overloading", "previous definition of %s in %s:%d:%d",
            ir_subroutine_name(sub), lsv->file_name, 0, 0);
        ir_ctx_add_exception(ctx, ex);
        ok = 0;
      }
    }
  }

  if (!ok) {
    ir_subroutine_free(sub);
  }

  return ok;
}

int ir_pass1_param_add(ir_pass1 *self, p_list list, ir_param *param) {
  UNUSED(self);
  list_push_back(list, param);
  debug("param_list added param %s", ir_param_name(param));
  return 1;
}
