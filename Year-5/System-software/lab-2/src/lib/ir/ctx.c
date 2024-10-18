#include "ir/ctx.h"

#include "exception.h"
#include "ir/functions.h"
#include "util/container_funcs.h"
#include "util/log.h"
#include "util/macro.h"
#include <assert.h>
#include <stdlib.h>

static void container_delete_sub(void *_data) {
  if (_data != NULL) {
    debug("subroutine free");
    ir_subroutine *sub = _data;
    ir_subroutine_free(sub);
  }
}

static int container_cmp_sub(const void *_lsv, const void *_rsv) {
  const ir_subroutine *lsv = _lsv;
  const ir_subroutine *rsv = _rsv;
  return ir_subroutine_cmp(lsv, rsv) != IR_SUBROUTINE_CMP_SAME;
}

static void container_delete_exc(void *_data) {
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
  ctx->subroutines =
      list_new(container_cmp_sub, container_new_move, container_delete_sub, 0);
  ctx->exceptions = list_new(container_cmp_false, container_new_move,
                             container_delete_exc, 0);
  ctx->free = ir_ctx_free;
  return ctx;
}
