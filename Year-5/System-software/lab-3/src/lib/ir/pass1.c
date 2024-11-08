#include "pass1.h"
#include "exception/exception.h"
#include "util/log.h"
#include "util/macro.h"

void ir_pass1_init(ir_pass1 *self, struct ir_ctx *ctx) {
  self->base.kind = IR_PASS_1;
  self->ctx = ctx;
}

void ir_pass1_deinit(ir_pass1 *self) { self->ctx = NULL; }

ir_t_base *ir_pass1_t_bool_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_BOOL);
}
ir_t_base *ir_pass1_t_byte_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_BYTE);
}
ir_t_base *ir_pass1_t_int_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_INT);
}
ir_t_base *ir_pass1_t_uint_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_UINT);
}
ir_t_base *ir_pass1_t_long_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_LONG);
}
ir_t_base *ir_pass1_t_ulong_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_ULONG);
}
ir_t_base *ir_pass1_t_char_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_CHAR);
}
ir_t_base *ir_pass1_t_string_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_STRING);
}

ir_t_base *ir_pass1_t_custom_new(ir_pass1 *self, const char *name) {
  UNUSED(self);
  return (ir_t_base *)ir_t_custom_new(name);
}
ir_t_base *ir_pass1_t_array_new(ir_pass1 *self, size_t dimension,
                                ir_t_base *type) {
  UNUSED(self);
  return (ir_t_base *)ir_t_array_new(dimension, type);
}

ir_var *ir_pass1_param_new(ir_pass1 *self, ir_term_id *id, ir_t_base *type) {
  UNUSED(self);
  return ir_var_new(id, type);
}

p_list ir_pass1_params_new(ir_pass1 *self) {
  UNUSED(self);
  return ir_var_list_new();
}

int ir_pass1_param_add(ir_pass1 *self, p_list list, ir_var *param) {
  UNUSED(self);
  list_push_back(list, param);
  debug("param_list added param %s", ir_var_name(param));
  return 1;
}

ir_signature *ir_pass1_signature_new(ir_pass1 *self, ir_term_id *id,
                                     p_list params, ir_t_base *ret) {
  UNUSED(self);
  return ir_signature_new(id, params, ret);
}

ir_subroutine *ir_pass1_subroutine_new(const ir_pass1 *self,
                                       ir_signature   *signature) {
  UNUSED(self);
  return ir_subroutine_new(self->file_name, signature, NULL, NULL);
}

static int ir_pass1_subroutine_validate(ir_pass1 *self, ir_subroutine *sub) {
  p_list params = sub->signature->params;
  for (list_it it = params->head; it; it = it->next) {
    for (list_it it2 = it->next; it2; it2 = it2->next) {
      if (!ir_var_cmp(it->data, it2->data)) {
        const int  subtype = EXCEPTION_IR_SUBROUTINE_PARAMETER_DUPLICATION;
        exception *ex =
            exception_new_f(EXCEPTION_IR, subtype, sub->file_name, 0, 0,
                            exception_subtype_ir_str(subtype),
                            "function %s parameter %s is duplicated",
                            ir_subroutine_name(sub), ir_var_name(it->data));
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
        const int  subtype = EXCEPTION_IR_SUBROUTINE_OVERLOADING;
        exception *ex =
            exception_new_f(EXCEPTION_IR, subtype, sub->file_name, 0, 0,
                            exception_subtype_ir_str(subtype),
                            "previous definition of %s in %s:%d:%d",
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
