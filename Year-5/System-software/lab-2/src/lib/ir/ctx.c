#include "ir/ctx.h"

#include "exception.h"
#include "ir/functions.h"
#include "util/list_funcs.h"
#include "util/macro.h"
#include <assert.h>
#include <stdlib.h>

// move
static void *sub_list_new(void *_data) {
  ir_subroutine *sub = _data;
  return sub;
}

static void sub_list_delete(void *_data) {
  if (_data != NULL) {
    ir_subroutine *sub = _data;
    ir_subroutine_free(sub);
  }
}

static int sub_list_cmp(const void *_lsv, const void *_rsv) {
  const ir_subroutine *lsv = _lsv;
  const ir_subroutine *rsv = _rsv;
  return ir_subroutine_cmp(lsv, rsv) != IR_SUBROUTINE_CMP_SAME;
}

// move
static void *exc_list_new(void *_data) {
  exception *exc = _data;
  return exc;
}

static void exc_list_delete(void *_data) {
  if (_data != NULL) {
    exception *exc = _data;
    exc->free(exc);
  }
}

static void inter_free(ir_ctx *inter) {
  if (inter != NULL) {
    inter->subroutines->free(inter->subroutines);
    inter->exceptions->free(inter->exceptions);
    free(inter);
  }
}

void ctx_add_exception(ir_ctx *ctx, exception *ex) {
  ctx->exceptions->put(ctx->exceptions, ex);
}

// TODO: maybe later allow overloading
int ctx_add_subroutine(ir_ctx *ctx, ir_subroutine *subroutine) {
  int ret = 0;
  for (list_iter it = ctx->subroutines->head; it != NULL; it = it->next) {
    ir_subroutine *lsv = it->data;
    int cmp = ir_subroutine_cmp(lsv, subroutine);
    int status = ir_subroutine_status(lsv);

    if ((cmp & IR_SUBROUTINE_CMP_NAME) != 0) { // is names are different
      continue;
    }

    if (cmp != IR_SUBROUTINE_CMP_SAME) {
      exception *ex = exception_new_f(
          EXCEPTION_IR, EXCEPTION_IR_FUNCTION_OVERLOADING,
          subroutine->file_name, 0, 0, "function overloading is not allowed",
          "previous definition in %s %d:%d", lsv->file_name, 0, 0);
      ctx_add_exception(ctx, ex);
      break;
    }

    if (status == IR_SUBROUTINE_STATUS_DEFINITION) {
      // TODO: may replace 0-s with actual location
      exception *ex = exception_new_f(
          EXCEPTION_IR, EXCEPTION_IR_FUNCTION_REDEFINITION,
          subroutine->file_name, 0, 0, "function redefinition",
          "previous definition in %s %d:%d", lsv->file_name, 0, 0);
      ctx_add_exception(ctx, ex);
      break;
    }

    ctx->subroutines->replace(ctx->subroutines, it, subroutine);
    ret = 1;
    break;
  }
  return ret;
}

ir_ctx *ir_ctx_new() {
  ir_ctx *ctx = MALLOC(ir_ctx);
  ctx->subroutines = list_new(sub_list_cmp, sub_list_new, sub_list_delete, 0);
  ctx->exceptions = list_new(list_false_cmp, exc_list_new, exc_list_delete, 0);
  ctx->free = inter_free;
  return ctx;
}
