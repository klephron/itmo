#include "ctx_type.h"
#include "exception/exception.h"
#include "util/macro.h"

static void hir_error_add(hir_ctx *ctx, exception_subtype_hir subtype,
                          const span *span, const char *format, ...) {
  exception *exc;
  va_list    args;
  va_start(args, format);

  exc = exception_new_v(EXCEPTION_LEVEL_ERROR, EXCEPTION_HIR, subtype,
                        span->source, span->line_start, span->pos_start,
                        exception_subtype_hir_str(subtype), format, args);
  list_exception_push_back(ctx->exceptions, exc);

  va_end(args);
}

static int hir_ctx_type_type(hir_ctx *ctx, type_base *type) {
  int ok = 1;
  if (!type) {
    return ok;
  }
  switch (type->type) {
  case TYPE_CUSTOM: {
    type_custom *custom = (type_custom *)type;
    const span  *cs = custom->base.span;
    hir_error_add(ctx, EXCEPTION_HIR_TYPE_UNSUPPORTED, cs,
                  "type %s at %s %d:%d %d:%d is unsupported", custom->name,
                  cs->source, cs->line_start, cs->pos_start, cs->line_end,
                  cs->pos_end);
    ok = 0;
  } break;
  case TYPE_ARRAY:
    ok &= hir_ctx_type_type(ctx, ((type_array *)type)->elem_type);
    break;
  case TYPE_CALLABLE: {
    type_callable *callable = (type_callable *)type;
    ok &= hir_ctx_type_type(ctx, callable->ret_type);
    // NOTE: params are local symbols, no need to check
    // for (list_type_it it = list_type_begin(callable->param_types);
    // !it.end(&it);
    //      it.next(&it)) {
    //   ok &= hir_ctx_type_type(ctx, it.get(&it));
    // }
  } break;
  default:
    break;
  }
  return ok;
}

static int hir_ctx_type_subroutine_decl(hir_ctx *ctx, hir_subroutine *sub) {
  int         ok = 1;
  const span *sub_span = sub->base.span;
  for (list_hir_param_it it = list_hir_param_begin(sub->params); !it.end(&it);
       it.next(&it)) {
    hir_param  *param = it.get(&it);
    const span *ps = param->base.span;
    if (!param->type) {
      hir_error_add(ctx, EXCEPTION_HIR_SUBROUTINE_DECL_INVALID, sub_span,
                    "param %s at %s %d:%d %d:%d type is required",
                    param->id->name, ps->source, ps->line_start, ps->pos_start,
                    ps->line_end, ps->pos_end);
      ok = 0;
    }
  }
  if (!sub->ret_type) {
    hir_error_add(ctx, EXCEPTION_HIR_SUBROUTINE_DECL_INVALID, sub_span,
                  "return type is required");
    ok = 0;
  }
  return ok;
}

void hir_ctx_type_all(hir_ctx *ctx) {
  for (list_hir_symbol_it it = list_hir_symbol_begin(ctx->hir->globals);
       !it.end(&it); it.next(&it)) {
    const hir_symbol *symbol = it.get(&it);
    hir_ctx_type_type(ctx, symbol->type);
  }
  for (list_hir_symbol_it it = list_hir_symbol_begin(ctx->hir->locals);
       !it.end(&it); it.next(&it)) {
    const hir_symbol *symbol = it.get(&it);
    hir_ctx_type_type(ctx, symbol->type);
  }

  for (list_hir_subroutine_it it =
           list_hir_subroutine_begin(ctx->hir->subroutines);
       !it.end(&it); it.next(&it)) {
    hir_subroutine *subroutine = it.get(&it);
    if (!subroutine->body) {
      hir_ctx_type_subroutine_decl(ctx, subroutine);
    }
  }
}
