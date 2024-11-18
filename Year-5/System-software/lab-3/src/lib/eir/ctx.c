#include "ctx.h"
#include "eir/eir.h"
#include "exception/container.h"
#include "hir/node_expr.h"
#include "util/log.h"
#include "util/macro.h"

static eir_expr_base *eir_ctx_build_expr_unary(eir_ctx              *ctx,
                                               const hir_expr_unary *unary);
static eir_expr_base *eir_ctx_build_expr_binary(eir_ctx               *ctx,
                                                const hir_expr_binary *binary);
static eir_expr_base *eir_ctx_build_expr_lit(eir_ctx            *ctx,
                                             const hir_expr_lit *lit);
static eir_expr_base *eir_ctx_build_expr_id(eir_ctx           *ctx,
                                            const hir_expr_id *id);
static eir_expr_base *eir_ctx_build_expr_call(eir_ctx             *ctx,
                                              const hir_expr_call *call);
static eir_expr_base *eir_ctx_build_expr_index(eir_ctx              *ctx,
                                               const hir_expr_index *index);
static eir_expr_base *eir_ctx_build_expr_builtin(eir_ctx                *ctx,
                                                 const hir_expr_builtin *cast);

static eir_scope *eir_ctx_build_stmt_if(eir_ctx *self, const hir_stmt_if *stmt);
static eir_scope *eir_ctx_build_stmt_block(eir_ctx              *self,
                                           const hir_stmt_block *stmt);
static eir_scope *eir_ctx_build_stmt_while(eir_ctx              *self,
                                           const hir_stmt_while *stmt);
static eir_scope *eir_ctx_build_stmt_do(eir_ctx *self, const hir_stmt_do *stmt);
static eir_scope *eir_ctx_build_stmt_break(eir_ctx              *self,
                                           const hir_stmt_break *stmt);
static eir_scope *eir_ctx_build_stmt_expr(eir_ctx             *self,
                                          const hir_stmt_expr *stmt);

static size_t eir_ctx_next_block_id(eir_ctx *ctx) { return ctx->block_cnt++; }

eir_scope *eir_scope_new(eir_block_base *begin, eir_block_base *cur,
                         eir_block_base *end) {
  eir_scope *self = MALLOC(eir_scope);
  self->begin = begin;
  self->cur = cur;
  self->end = end;
  return self;
}

void eir_scope_free(eir_scope *self) {
  if (self) {
    free(self);
  }
}

eir_ctx *eir_ctx_new(eir *eir) {
  eir_ctx *self = MALLOC(eir_ctx);
  self->eir = eir;
  self->exceptions = list_exception_new();
  self->block_cnt = 0;
  return self;
}

void eir_ctx_reset_exceptions(eir_ctx *self) {
  list_exception_free(self->exceptions);
  self->exceptions = list_exception_new();
}

void eir_ctx_free(eir_ctx *self) {
  if (self) {
    list_exception_free(self->exceptions);
    self->exceptions = NULL;
    self->eir = NULL;
    free(self);
  }
}

// EXCEPTIONS
// static void eir_error_null(eir_ctx *ctx, const span *pspan, const char
// *format,
// ...) {
//   exception *exc;
//   va_list    args;
//   va_start(args, format);

//   exc = exception_new_v(
//       EXCEPTION_LEVEL_ERROR, EXCEPTION_EIR, EXCEPTION_EIR_NULL,
//       pspan->source, pspan->line_start, pspan->pos_start,
//       exception_subtype_eir_str(EXCEPTION_EIR_NULL), format, args);
//   list_exception_push_back(ctx->exceptions, exc);

//   va_end(args);
// }

static void eir_error_rvalue_assignment(eir_ctx *ctx, const span *span) {
  exception *exc;
  exc = exception_new_f(
      EXCEPTION_LEVEL_ERROR, EXCEPTION_EIR, EXCEPTION_EIR_NULL, span->source,
      span->line_start, span->pos_start,
      exception_subtype_eir_str(EXCEPTION_EIR_NULL),
      "expression is rvalue at %s %zu:%zu %zu:%zu", span->source,
      span->line_start, span->pos_start, span->line_end, span->pos_end);
  list_exception_push_back(ctx->exceptions, exc);
}

static void eir_error_bad_break(eir_ctx *ctx, const span *span) {
  exception *exc;
  exc = exception_new_f(
      EXCEPTION_LEVEL_ERROR, EXCEPTION_EIR, EXCEPTION_EIR_BAD_BREAK,
      span->source, span->line_start, span->pos_start,
      exception_subtype_eir_str(EXCEPTION_EIR_BAD_BREAK),
      "can't break at %s %zu:%zu %zu:%zu", span->source, span->line_start,
      span->pos_start, span->line_end, span->pos_end);
  list_exception_push_back(ctx->exceptions, exc);
}

// EXPR
static eir_expr_base *eir_ctx_build_expr(eir_ctx             *ctx,
                                         const hir_expr_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case HIR_EXPR_UNARY:
      return eir_ctx_build_expr_unary(ctx, (hir_expr_unary *)generic);
    case HIR_EXPR_BINARY:
      return eir_ctx_build_expr_binary(ctx, (hir_expr_binary *)generic);
    case HIR_EXPR_LITERAL:
      return eir_ctx_build_expr_lit(ctx, (hir_expr_lit *)generic);
    case HIR_EXPR_IDENTIFIER:
      return eir_ctx_build_expr_id(ctx, (hir_expr_id *)generic);
    case HIR_EXPR_CALL:
      return eir_ctx_build_expr_call(ctx, (hir_expr_call *)generic);
    case HIR_EXPR_INDEX:
      return eir_ctx_build_expr_index(ctx, (hir_expr_index *)generic);
    case HIR_EXPR_BUILTIN:
      return eir_ctx_build_expr_builtin(ctx, (hir_expr_builtin *)generic);
    default:
      error("unexpected expr type %d %p", generic->kind, generic);
      return NULL;
    }
  }
  return NULL;
}

static eir_expr_base *eir_ctx_build_expr_unary(eir_ctx              *ctx,
                                               const hir_expr_unary *unary) {
  if (unary && unary->first) {
    const span      *u_span = unary->base.base.span;
    const type_base *u_type = unary->base.type;

    // force load first arg
    eir_expr_base *first = eir_ctx_build_expr(ctx, unary->first);
    eir_expr_base *first_load = NULL;
    if (first && first->kind == EIR_EXPR_IDENTIFIER) {
      first_load = (eir_expr_base *)eir_expr_unary_new(
          span_copy(first->span), type_copy(first->type), EIR_EXPR_UNARY_LOAD,
          first);
    }

    // default implementation (no changes)
    eir_expr_unary_enum op_nch;
    switch (unary->op) {
    case HIR_EXPR_UNARY_PLUS:
      op_nch = EIR_EXPR_UNARY_PLUS;
      break;
    case HIR_EXPR_UNARY_MINUS:
      op_nch = EIR_EXPR_UNARY_MINUS;
      break;
    case HIR_EXPR_UNARY_LOGICAL_NOT:
      op_nch = EIR_EXPR_UNARY_LOGICAL_NOT;
      break;
    case HIR_EXPR_UNARY_BITWISE_NOT:
      op_nch = EIR_EXPR_UNARY_BITWISE_NOT;
      break;
    default:
      op_nch = EIR_EXPR_UNARY_UNKNOWN;
      break;
    }

    if (op_nch != EIR_EXPR_UNARY_UNKNOWN) {
      return (eir_expr_base *)eir_expr_unary_new(
          span_copy(u_span), type_copy(u_type), op_nch,
          first_load ? first_load : first);
    }

    // rewriting
    switch (unary->op) {
    case HIR_EXPR_UNARY_INC:
    case HIR_EXPR_UNARY_DEC: {
      if (!first_load) {
        eir_error_rvalue_assignment(ctx, first->span);
        eir_expr_free(first);
        return NULL;
      }

      eir_expr_unary_enum inc_dec;
      if (unary->op == HIR_EXPR_UNARY_INC) {
        inc_dec = EIR_EXPR_UNARY_INC;
      } else {
        inc_dec = EIR_EXPR_UNARY_DEC;
      }

      eir_expr_base *expr_inc_dec = (eir_expr_base *)eir_expr_unary_new(
          span_copy(u_span), type_copy(u_type), inc_dec, first_load);

      return (eir_expr_base *)eir_expr_binary_new(
          span_copy(u_span), type_copy(u_type), EIR_EXPR_BINARY_STORE,
          eir_ctx_build_expr(ctx, unary->first), expr_inc_dec);
    } break;

    default:
      error("unhandled type %d", unary->op);
    };
  }
  return NULL;
}

static eir_expr_base *eir_ctx_build_expr_binary(eir_ctx               *ctx,
                                                const hir_expr_binary *binary) {
  if (binary && binary->first && binary->second) {
    const span      *b_span = binary->base.base.span;
    const type_base *b_type = binary->base.type;

    eir_expr_base *first = eir_ctx_build_expr(ctx, binary->first);

    // force load second argument
    eir_expr_base *second = eir_ctx_build_expr(ctx, binary->second);
    eir_expr_base *second_load = NULL;
    if (second && second->kind == EIR_EXPR_IDENTIFIER) {
      second_load = (eir_expr_base *)eir_expr_unary_new(
          span_copy(second->span), type_copy(second->type), EIR_EXPR_UNARY_LOAD,
          second);
    }

    // default implementation (no changes)
    eir_expr_binary_enum op_nch;
    switch (binary->op) {
    case HIR_EXPR_BINARY_LOGICAL_OR:
      op_nch = EIR_EXPR_BINARY_LOGICAL_OR;
      break;
    case HIR_EXPR_BINARY_LOGICAL_AND:
      op_nch = EIR_EXPR_BINARY_LOGICAL_AND;
      break;
    case HIR_EXPR_BINARY_BITWISE_OR:
      op_nch = EIR_EXPR_BINARY_BITWISE_OR;
      break;
    case HIR_EXPR_BINARY_BITWISE_XOR:
      op_nch = EIR_EXPR_BINARY_BITWISE_XOR;
      break;
    case HIR_EXPR_BINARY_BITWISE_AND:
      op_nch = EIR_EXPR_BINARY_BITWISE_AND;
      break;
    case HIR_EXPR_BINARY_EQUALS:
      op_nch = EIR_EXPR_BINARY_EQUALS;
      break;
    case HIR_EXPR_BINARY_NOT_EQUALS:
      op_nch = EIR_EXPR_BINARY_NOT_EQUALS;
      break;
    case HIR_EXPR_BINARY_LESS:
      op_nch = EIR_EXPR_BINARY_LESS;
      break;
    case HIR_EXPR_BINARY_LESS_EQUALS:
      op_nch = EIR_EXPR_BINARY_LESS_EQUALS;
      break;
    case HIR_EXPR_BINARY_GREATER:
      op_nch = EIR_EXPR_BINARY_GREATER;
      break;
    case HIR_EXPR_BINARY_GREATER_EQUALS:
      op_nch = EIR_EXPR_BINARY_GREATER_EQUALS;
      break;
    case HIR_EXPR_BINARY_BITWISE_SHIFT_LEFT:
      op_nch = EIR_EXPR_BINARY_BITWISE_SHIFT_LEFT;
      break;
    case HIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT:
      op_nch = EIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT;
      break;
    case HIR_EXPR_BINARY_ADD:
      op_nch = EIR_EXPR_BINARY_ADD;
      break;
    case HIR_EXPR_BINARY_SUB:
      op_nch = EIR_EXPR_BINARY_SUB;
      break;
    case HIR_EXPR_BINARY_MUL:
      op_nch = EIR_EXPR_BINARY_MUL;
      break;
    case HIR_EXPR_BINARY_DIV:
      op_nch = EIR_EXPR_BINARY_DIV;
      break;
    case HIR_EXPR_BINARY_MOD:
      op_nch = EIR_EXPR_BINARY_MOD;
      break;
    default:
      op_nch = EIR_EXPR_BINARY_UNKNOWN;
      break;
    }

    if (op_nch != EIR_EXPR_BINARY_UNKNOWN) {
      // force load first
      eir_expr_base *first_load = NULL;
      if (first && first->kind == EIR_EXPR_IDENTIFIER) {
        first_load = (eir_expr_base *)eir_expr_unary_new(
            span_copy(first->span), type_copy(first->type), EIR_EXPR_UNARY_LOAD,
            first);
      }
      return (eir_expr_base *)eir_expr_binary_new(
          span_copy(b_span), type_copy(b_type), op_nch,
          first_load ? first_load : first, second_load ? second_load : second);
    }

    // rewriting
    switch (binary->op) {
    case HIR_EXPR_BINARY_ASSIGN:
      if (first && first->kind != EIR_EXPR_IDENTIFIER) {
        eir_error_rvalue_assignment(ctx, first->span);
        eir_expr_free(second);
        return NULL;
      }

      return (eir_expr_base *)eir_expr_binary_new(
          span_copy(b_span), type_copy(b_type), EIR_EXPR_BINARY_STORE, first,
          second_load ? second_load : second);
    default:
      error("unhandled type %d", binary->op);
    }
  }
  return NULL;
}

static eir_expr_base *eir_ctx_build_expr_lit(eir_ctx            *ctx,
                                             const hir_expr_lit *lit) {
  UNUSED(ctx);
  if (lit) {
    return (eir_expr_base *)eir_expr_lit_new(span_copy(lit->base.base.span),
                                             type_copy(lit->base.type),
                                             hir_lit_copy(lit->lit));
  }
  return NULL;
}

static eir_expr_base *eir_ctx_build_expr_id(eir_ctx           *ctx,
                                            const hir_expr_id *id) {
  UNUSED(ctx);
  if (id) {
    return (eir_expr_base *)eir_expr_id_new(
        span_copy(id->base.base.span), type_copy(id->base.type), id->binding);
  }
  return NULL;
}

static eir_expr_base *eir_ctx_build_expr_call(eir_ctx             *ctx,
                                              const hir_expr_call *call) {
  if (call) {
    list_eir_expr *list = list_eir_expr_new();
    for (list_hir_expr_it it = list_hir_expr_begin(call->args); !it.end(&it);
         it.next(&it)) {
      list_eir_expr_push_back(list, eir_ctx_build_expr(ctx, it.get(&it)));
    }

    return (eir_expr_base *)eir_expr_call_new(
        span_copy(call->base.base.span), type_copy(call->base.type),
        eir_ctx_build_expr(ctx, call->callee), list);
  }
  return NULL;
}

static eir_expr_base *eir_ctx_build_expr_index(eir_ctx              *ctx,
                                               const hir_expr_index *index) {
  if (index) {
    list_eir_expr *list = list_eir_expr_new();
    for (list_hir_expr_it it = list_hir_expr_begin(index->args); !it.end(&it);
         it.next(&it)) {
      list_eir_expr_push_back(list, eir_ctx_build_expr(ctx, it.get(&it)));
    }

    return (eir_expr_base *)eir_expr_index_new(
        span_copy(index->base.base.span), type_copy(index->base.type),
        eir_ctx_build_expr(ctx, index->indexed), list);
  }
  return NULL;
}

static eir_expr_base *
eir_ctx_build_expr_builtin(eir_ctx *ctx, const hir_expr_builtin *builtin) {
  if (builtin) {
    list_eir_expr *args = list_eir_expr_new();
    for (list_hir_expr_it it = list_hir_expr_begin(builtin->args); !it.end(&it);
         it.next(&it)) {
      list_eir_expr_push_back(args, eir_ctx_build_expr(ctx, it.get(&it)));
    }

    eir_expr_builtin_enum kind;
    switch (builtin->kind) {
    case HIR_EXPR_BUILTIN_CAST:
      kind = EIR_EXPR_BUILTIN_CAST;
      break;
    case HIR_EXPR_BUILTIN_NEW:
      kind = EIR_EXPR_BUILTIN_NEW;
      break;
    case HIR_EXPR_BUILTIN_FREE:
      kind = EIR_EXPR_BUILTIN_FREE;
      break;
    default:
      error("unknown builtin kind %d %p", builtin->kind, builtin);
      kind = EIR_EXPR_BUILTIN_UNKNOWN;
      break;
    }

    return (eir_expr_base *)eir_expr_builtin_new(
        span_copy(builtin->base.base.span), type_copy(builtin->base.type), kind,
        args);
  }
  return NULL;
}

static eir_scope *eir_ctx_build_stmt(eir_ctx             *ctx,
                                     const hir_stmt_base *generic) {
  if (generic) {
    switch (generic->kind) {
    case HIR_STMT_IF:
      return eir_ctx_build_stmt_if(ctx, (hir_stmt_if *)generic);
    case HIR_STMT_BLOCK:
      return eir_ctx_build_stmt_block(ctx, (hir_stmt_block *)generic);
    case HIR_STMT_WHILE:
      return eir_ctx_build_stmt_while(ctx, (hir_stmt_while *)generic);
    case HIR_STMT_DO:
      return eir_ctx_build_stmt_do(ctx, (hir_stmt_do *)generic);
    case HIR_STMT_BREAK:
      return eir_ctx_build_stmt_break(ctx, (hir_stmt_break *)generic);
    case HIR_STMT_EXPR:
      return eir_ctx_build_stmt_expr(ctx, (hir_stmt_expr *)generic);
    default:
      error("unexpected hir_stmt kind %d %p", generic->kind, generic);
      return NULL;
    }
  }
  return NULL;
}

static eir_scope *eir_ctx_build_stmt_if(eir_ctx *ctx, const hir_stmt_if *stmt) {
  eir_scope *parent = list_eir_scope_front(ctx->scope_stack);

  eir_block_base *end = (eir_block_base *)eir_block_next_new(
      eir_ctx_next_block_id(ctx), NULL, NULL, NULL);
  eir_scope *scope = eir_scope_new(parent->cur, parent->cur, end);

  list_eir_scope_push_front(ctx->scope_stack, scope);

  eir_expr_base *cur_cond = eir_ctx_build_expr(ctx, stmt->cond);
  eir_scope     *cur_je = eir_ctx_build_stmt(ctx, stmt->je);
  eir_scope     *cur_jz = eir_ctx_build_stmt(ctx, stmt->jz);

  list_eir_scope_pop_front(ctx->scope_stack);

  if (cur_je) {
    eir_block_set_next(cur_je->end, scope->end);
  }

  if (cur_jz) {
    eir_block_set_next(cur_jz->end, scope->end);
  }

  eir_block_base *block_cond = (eir_block_base *)eir_block_cond_new(
      eir_ctx_next_block_id(ctx), span_copy(stmt->base.base.span), cur_cond,
      cur_je ? cur_je->begin : scope->end, cur_jz ? cur_jz->begin : scope->end);

  eir_scope_free(cur_je);
  eir_scope_free(cur_jz);

  eir_block_set_next(scope->begin, block_cond);
  scope->begin = block_cond;

  return scope;
}

static eir_scope *eir_ctx_build_stmt_block(eir_ctx              *ctx,
                                           const hir_stmt_block *stmt) {
  if (!stmt) {
    return NULL;
  }

  eir_scope *parent = list_eir_scope_front(ctx->scope_stack);

  eir_block_base *end = (eir_block_base *)eir_block_next_new(
      eir_ctx_next_block_id(ctx), NULL, NULL, NULL);
  eir_scope *scope = eir_scope_new(parent->cur, parent->cur, end);

  list_eir_scope_push_front(ctx->scope_stack, scope);

  for (list_hir_stmt_it it = list_hir_stmt_begin(stmt->stmts); !it.end(&it);
       it.next(&it)) {
    hir_stmt_base *stmt = it.get(&it);
    eir_scope     *child = eir_ctx_build_stmt(ctx, stmt);
    if (child) {
      eir_block_set_next(scope->cur, child->begin);
      scope->cur = child->end;
      eir_scope_free(child);
    }
  }

  list_eir_scope_pop_front(ctx->scope_stack);

  eir_block_set_next(scope->cur, scope->end);
  scope->cur = scope->end;

  scope->begin = eir_block_get_next(scope->begin);
  return scope;
}

static eir_scope *eir_ctx_build_stmt_while(eir_ctx              *ctx,
                                           const hir_stmt_while *stmt) {
  eir_scope *parent = list_eir_scope_front(ctx->scope_stack);

  eir_block_base *end = (eir_block_base *)eir_block_next_new(
      eir_ctx_next_block_id(ctx), NULL, NULL, NULL);
  eir_scope *scope = eir_scope_new(parent->cur, parent->cur, end);

  list_eir_scope_push_front(ctx->scope_stack, scope);
  list_eir_block_push_front(ctx->break_stack, end);

  eir_expr_base *expr = eir_ctx_build_expr(ctx, stmt->cond);
  eir_scope     *child = eir_ctx_build_stmt(ctx, stmt->stmt);

  list_eir_scope_pop_front(ctx->scope_stack);
  list_eir_block_pop_front(ctx->break_stack);

  eir_block_base *block_cond = (eir_block_base *)eir_block_cond_new(
      eir_ctx_next_block_id(ctx), span_copy(stmt->base.base.span), expr,
      child ? child->begin : scope->end, scope->end);

  if (child) {
    eir_block_set_next(child->end, block_cond);
    eir_scope_free(child);
  }

  eir_block_set_next(scope->begin, block_cond);
  scope->begin = block_cond;

  return scope;
}

static eir_scope *eir_ctx_build_stmt_do(eir_ctx *ctx, const hir_stmt_do *stmt) {
  eir_scope *parent = list_eir_scope_front(ctx->scope_stack);

  eir_block_base *end = (eir_block_base *)eir_block_next_new(
      eir_ctx_next_block_id(ctx), NULL, NULL, NULL);
  eir_scope *scope = eir_scope_new(parent->cur, parent->cur, end);

  list_eir_scope_push_front(ctx->scope_stack, scope);
  list_eir_block_push_front(ctx->break_stack, end);

  eir_scope     *child = eir_ctx_build_stmt(ctx, stmt->stmt);
  eir_expr_base *expr = eir_ctx_build_expr(ctx, stmt->cond);

  list_eir_scope_pop_front(ctx->scope_stack);
  list_eir_block_pop_front(ctx->break_stack);

  eir_block_base *cur;
  if (stmt->positive) {
    cur = (eir_block_base *)eir_block_cond_new(
        eir_ctx_next_block_id(ctx), span_copy(stmt->base.base.span), expr,
        child ? child->begin : scope->end, scope->end);
  } else {
    cur = (eir_block_base *)eir_block_cond_new(
        eir_ctx_next_block_id(ctx), span_copy(stmt->base.base.span), expr,
        scope->end, child ? child->begin : scope->end);
  }

  if (child) {
    eir_block_set_next(child->end, cur);
  }

  eir_block_set_next(scope->begin, child ? child->begin : scope->end);
  scope->begin = child ? child->begin : scope->end;

  eir_scope_free(child);

  return scope;
}

static eir_scope *eir_ctx_build_stmt_break(eir_ctx              *ctx,
                                           const hir_stmt_break *stmt) {
  const span *span = stmt->base.base.span;

  if (list_eir_block_size(ctx->break_stack) == 0) {
    eir_error_bad_break(ctx, stmt->base.base.span);
    return NULL;
  }

  eir_expr_base *expr_true = (eir_expr_base *)eir_expr_lit_new(
      span_copy(span), type_base_new(span_copy(span), TYPE_BOOL),
      hir_lit_new(span_copy(span), type_base_new(span_copy(span), TYPE_BOOL),
                  (hir_lit_u){.v_bool = 1}));

  eir_block_base *cur = (eir_block_base *)eir_block_cond_new(
      eir_ctx_next_block_id(ctx), span_copy(span), expr_true,
      list_eir_block_front(ctx->break_stack), NULL);

  return eir_scope_new(cur, cur, cur);
}

static eir_scope *eir_ctx_build_stmt_expr(eir_ctx             *ctx,
                                          const hir_stmt_expr *stmt) {
  eir_expr_base  *expr = eir_ctx_build_expr(ctx, stmt->expr);
  eir_block_base *cur = (eir_block_base *)eir_block_next_new(
      eir_ctx_next_block_id(ctx), span_copy(stmt->base.base.span), expr, NULL);

  return eir_scope_new(cur, cur, cur);
}

static eir_subroutine *eir_ctx_build_subroutine(eir_ctx              *ctx,
                                                const hir_subroutine *hir_sub) {
  if (hir_sub->body) {
    ctx->scope_stack = list_eir_scope_new();
    ctx->break_stack = list_eir_block_new();

    eir_block_base *scope_begin, *scope_cur, *scope_end;
    scope_begin = (eir_block_base *)eir_block_next_new(
        eir_ctx_next_block_id(ctx), NULL, NULL, NULL);
    scope_end = (eir_block_base *)eir_block_next_new(eir_ctx_next_block_id(ctx),
                                                     NULL, NULL, NULL);
    scope_cur = scope_begin;

    eir_scope *scope = eir_scope_new(scope_begin, scope_cur, scope_end);
    list_eir_scope_push_front(ctx->scope_stack, scope);

    // do body
    eir_scope *child = eir_ctx_build_stmt_block(ctx, hir_sub->body);

    if (child) {
      scope->cur = child->end;
      eir_scope_free(child);
    }

    eir_block_set_next(scope->cur, scope->end);

    list_eir_block_free(ctx->break_stack);
    list_eir_scope_free(ctx->scope_stack);

    return eir_subroutine_new(scope_begin, hir_sub->binding);
  }

  return eir_subroutine_new(NULL, hir_sub->binding);
}

// 1. copy referencees to global and local symbol tables
// 2. rebuild hir to eir (will change some tree nodes)
void eir_ctx_build(eir_ctx *ctx, const hir *hir) {
  ctx->eir->globals = hir->globals;
  ctx->eir->locals = hir->locals;

  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !it.end(&it); it.next(&it)) {
    list_eir_subroutine_push_back(ctx->eir->subroutines,
                                  eir_ctx_build_subroutine(ctx, it.get(&it)));
  }
}
