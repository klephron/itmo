#include "inst_sub.h"
#include "compiler/codegen/exception.h"
#include "util/macro.h"

typedef struct cg_ctx_struct {
  cg_x86_64      *code;
  list_exception *exceptions;
} cg_ctx;

static void cg_ctx_init(cg_ctx *ctx, cg_x86_64 *code,
                        list_exception *exceptions) {
  ctx->code       = code;
  ctx->exceptions = exceptions;
}

static void cg_ctx_deinit(cg_ctx *ctx) {
  ctx->code       = NULL;
  ctx->exceptions = NULL;
}

cg_inst_sub_result cg_inst_sub(cg_x86_64 *code, const mir *mir) {
  cg_inst_sub_result result = {
      .exceptions = list_exception_new(),
  };

  cg_ctx ctx;
  cg_ctx_init(&ctx, code, result.exceptions);

  for (list_mir_subroutine_it it =
           list_mir_subroutine_begin(mir->imported_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *sub = GET(it);
    cg_exception_add_error(ctx.exceptions, EXCEPTION_CG_UNSUPPORTED_IMPORTED,
                           sub->symbol_ref->span,
                           "subroutine '%s' can't be instantiated",
                           sub->symbol_ref->name);
  }

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->defined_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *sub = GET(it);
  }

  cg_ctx_deinit(&ctx);

  return result;
}
