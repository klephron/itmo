#include "ir/ctx.h"

#include "exception.h"
#include "ir/functions.h"
#include "util/list_funcs.h"
#include "util/log.h"
#include "util/macro.h"
#include <assert.h>
#include <stdlib.h>

static void list_sub_delete(void *_data) {
  if (_data != NULL) {
    debug("subroutine free");
    ir_subroutine *sub = _data;
    ir_subroutine_free(sub);
  }
}

static int list_sub_cmp(const void *_lsv, const void *_rsv) {
  const ir_subroutine *lsv = _lsv;
  const ir_subroutine *rsv = _rsv;
  return ir_subroutine_cmp(lsv, rsv) != IR_SUBROUTINE_CMP_SAME;
}

static void list_exc_delete(void *_data) {
  if (_data != NULL) {
    debug("exception free");
    exception *exc = _data;
    exc->free(exc);
  }
}

static void ir_ctx_free(ir_ctx *ctx) {
  debug("ctx free");
  if (ctx != NULL) {
    ctx->subroutines->free(ctx->subroutines);
    ctx->exceptions->free(ctx->exceptions);
    free(ctx);
  }
}

void ir_ctx_add_exception(ir_ctx *ctx, exception *ex) {
  ctx->exceptions->put(ctx->exceptions, ex);
}

ir_ctx *ir_ctx_new() {
  ir_ctx *ctx = MALLOC(ir_ctx);
  ctx->subroutines = list_new(list_sub_cmp, list_move_new, list_sub_delete, 0);
  ctx->exceptions = list_new(list_false_cmp, list_move_new, list_exc_delete, 0);
  ctx->free = ir_ctx_free;
  return ctx;
}
