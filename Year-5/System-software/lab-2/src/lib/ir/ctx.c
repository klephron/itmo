#include "ir/ctx.h"

#include "exception.h"
#include "ir/function.h"
#include "util/container_util.h"
#include "util/log.h"
#include "util/macro.h"
#include <assert.h>
#include <stdlib.h>

static void container_delete_sub(void *_data) {
  if (_data) {
    /*debug("subroutine free");*/
    ir_subroutine *sub = _data;
    ir_subroutine_free(sub);
  }
}

static size_t container_hash_sub(void *_data) {
  ir_subroutine *sub = _data;
  return container_hash_chars((void *)ir_subroutine_name(sub));
}

// check only for identifiers
static int container_cmp_sub(const void *_lsv, const void *_rsv) {
  const ir_subroutine *lsv = _lsv;
  const ir_subroutine *rsv = _rsv;
  return (ir_subroutine_cmp(lsv, rsv) & IR_SUBROUTINE_CMP_NAME);
}

static void container_delete_exc(void *_data) {
  if (_data) {
    /*debug("exception free");*/
    exception *exc = _data;
    exception_free(exc);
  }
}

void ir_ctx_free(ir_ctx *self) {
  /*debug("ctx free");*/
  if (self) {
    hashset_free(self->subroutines);
    list_free(self->exceptions);
    free(self);
  }
}

void ir_ctx_add_exception(ir_ctx *self, exception *ex) {
  list_push_back(self->exceptions, ex);
}

ir_ctx *ir_ctx_new() {
  ir_ctx *ctx = MALLOC(ir_ctx);
  ctx->subroutines = hashset_new(container_cmp_sub, container_new_move,
                                 container_delete_sub, container_hash_sub);
  ctx->exceptions =
      list_new(container_cmp_false, container_new_move, container_delete_exc);
  return ctx;
}
