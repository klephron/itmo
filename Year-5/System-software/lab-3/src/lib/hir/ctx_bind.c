#include "ctx_bind.h"
#include "util/log.h"

// just in case of mangling or something else
static void hir_ctx_scope_insert_symbol(hir_ctx_scope    *self,
                                        const hir_symbol *symbol) {
  hir_ctx_scope_entry *entry = hir_ctx_scope_entry_new(symbol->name, symbol);
  hashset_hir_ctx_scope_entry_insert(self->hashset, entry);
}

static const hir_symbol *hir_ctx_scope_find_symbol(hir_ctx_scope *self,
                                                   const char    *name) {
  hashset_hir_ctx_scope_entry_it it = hashset_hir_ctx_scope_entry_find(
      self->hashset, &(hir_ctx_scope_entry){.name = name});
  if (it.end(&it)) {
    return NULL;
  }
  return it.get(&it)->binding;
}

static const hir_symbol *hir_ctx_find_symbol(hir_ctx *ctx, const char *name) {
  list_hir_ctx_scope *scopes = ctx->scopes;

  for (list_hir_ctx_scope_it it = list_hir_ctx_scope_begin(scopes);
       !it.end(&it); it.next(&it)) {
    hir_ctx_scope    *scope = it.get(&it);
    const hir_symbol *found = hir_ctx_scope_find_symbol(scope, name);
    if (found) {
      return found;
    }
  }
  return NULL;
}

static void hir_ctx_error_symbol_redefinition(hir_ctx          *ctx,
                                              const hir_symbol *found,
                                              const span       *cur_span) {
  exception *exc = exception_new_f(
      EXCEPTION_LEVEL_ERROR, EXCEPTION_HIR, EXCEPTION_HIR_SYMBOL_REDEFINITION,
      cur_span->source, cur_span->line_start, cur_span->pos_start,
      exception_subtype_hir_str(EXCEPTION_HIR_SYMBOL_REDEFINITION),
      "symbol %s already defined %s %d:%d %d:%d", found->name,
      found->decl_span->source, found->decl_span->line_start,
      found->decl_span->pos_start, found->decl_span->line_end,
      found->decl_span->pos_end);
  list_exception_push_back(ctx->exceptions, exc);
}

static void hir_ctx_error_symbol_undefined(hir_ctx *ctx, const char *name,
                                           const span *cur_span) {
  exception *exc = exception_new_f(
      EXCEPTION_LEVEL_ERROR, EXCEPTION_HIR, EXCEPTION_HIR_SYMBOL_UNDEFINED,
      cur_span->source, cur_span->line_start, cur_span->pos_start,
      exception_subtype_hir_str(EXCEPTION_HIR_SYMBOL_UNDEFINED),
      "symbol %s referenced at %s %d:%d %d:%d is not found", name,
      cur_span->source, cur_span->line_start, cur_span->pos_start,
      cur_span->line_end, cur_span->pos_end);
  list_exception_push_back(ctx->exceptions, exc);
}

static void hir_ctx_bind_expr(hir_ctx *ctx, hir_expr_base *generic) {
  if (!generic) {
    return;
  }
  switch (generic->kind) {
  case HIR_EXPR_UNARY: {
    hir_expr_unary *unary = (hir_expr_unary *)generic;
    hir_ctx_bind_expr(ctx, unary->first);
  } break;
  case HIR_EXPR_BINARY: {
    hir_expr_binary *binary = (hir_expr_binary *)generic;
    hir_ctx_bind_expr(ctx, binary->first);
    hir_ctx_bind_expr(ctx, binary->second);
  } break;
  case HIR_EXPR_LITERAL: {
  } break;
  case HIR_EXPR_IDENTIFIER: {
    hir_expr_id      *id = (hir_expr_id *)generic;
    const hir_symbol *found = hir_ctx_find_symbol(ctx, id->id->name);
    if (!found) {
      hir_ctx_error_symbol_undefined(ctx, id->id->name, id->base.base.span);
    } else {
      id->binding = found;
    }
  } break;
  case HIR_EXPR_CALL: {
    hir_expr_call *call = (hir_expr_call *)generic;
    hir_ctx_bind_expr(ctx, call->callee);
    for (list_hir_expr_it it = list_hir_expr_begin(call->args); !it.end(&it);
         it.next(&it)) {
      hir_expr_base *expr = it.get(&it);
      hir_ctx_bind_expr(ctx, expr);
    }
  } break;
  case HIR_EXPR_INDEX: {
    hir_expr_index *index = (hir_expr_index *)generic;
    hir_ctx_bind_expr(ctx, index->indexed);
    for (list_hir_expr_it it = list_hir_expr_begin(index->args); !it.end(&it);
         it.next(&it)) {
      hir_expr_base *expr = it.get(&it);
      hir_ctx_bind_expr(ctx, expr);
    }
  } break;
  case HIR_EXPR_CAST: {
    hir_expr_cast *index = (hir_expr_cast *)generic;
    hir_ctx_bind_expr(ctx, index->expr);
  } break;
  default:
    error("unknown expr type %d %p", generic->kind, generic);
    break;
  }
}

static void hir_ctx_bind_stmt(hir_ctx *ctx, hir_stmt_base *generic) {
  if (!generic) {
    return;
  }
  switch (generic->kind) {
  case HIR_STMT_IF: {
    hir_stmt_if *stmt = (hir_stmt_if *)generic;
    hir_ctx_bind_expr(ctx, stmt->cond);
    hir_ctx_bind_stmt(ctx, stmt->je);
    hir_ctx_bind_stmt(ctx, stmt->jz);
  } break;
  case HIR_STMT_BLOCK: {
    hir_stmt_block *stmt = (hir_stmt_block *)generic;

    hir_ctx_scope *root_scope = hir_ctx_scope_new(HIR_CTX_SCOPE_BLOCK);
    list_hir_ctx_scope_push_front(ctx->scopes, root_scope);

    for (list_hir_stmt_it it = list_hir_stmt_begin(stmt->stmts); !it.end(&it);
         it.next(&it)) {
      hir_stmt_base *stmt = it.get(&it);
      hir_ctx_bind_stmt(ctx, stmt);
    }

    hir_ctx_scope_free(list_hir_ctx_scope_pop_front(ctx->scopes));
  } break;
  case HIR_STMT_WHILE: {
    hir_stmt_while *stmt = (hir_stmt_while *)generic;
    hir_ctx_bind_expr(ctx, stmt->cond);
    hir_ctx_bind_stmt(ctx, stmt->stmt);
  } break;
  case HIR_STMT_DO: {
    hir_stmt_do *stmt = (hir_stmt_do *)generic;
    hir_ctx_bind_stmt(ctx, stmt->stmt);
    hir_ctx_bind_expr(ctx, stmt->cond);
  } break;
  case HIR_STMT_BREAK: {
  } break;
  case HIR_STMT_EXPR: {
    hir_stmt_expr *stmt = (hir_stmt_expr *)generic;
    hir_ctx_bind_expr(ctx, stmt->expr);
  } break;
  default:
    error("unknown stmt kind %d %p", generic->kind, generic);
    break;
  }
}

static void hir_ctx_bind_subroutine(hir_ctx *ctx, hir_subroutine *subroutine) {
  hir_ctx_scope *root_scope = hir_ctx_scope_new(HIR_CTX_SCOPE_SUBROUTINE);
  list_hir_ctx_scope_push_front(ctx->scopes, root_scope);

  for (list_hir_param_it it = list_hir_param_begin(subroutine->params);
       !it.end(&it); it.next(&it)) {
    hir_param  *param = it.get(&it);
    const char *name = param->id->name;

    const hir_symbol *found = hir_ctx_find_symbol(ctx, name);
    if (found) {
      hir_ctx_error_symbol_redefinition(ctx, found, param->base.span);
    } else {
      hir_symbol *symbol = hir_symbol_new(
          strdup(name), HIR_BINDING_SUBROUTINE, subroutine->binding,
          type_copy(param->type), span_copy(param->base.span));
      list_hir_symbol_push_back(ctx->hir->locals, symbol);

      hir_ctx_scope_insert_symbol(root_scope, symbol);
      param->binding = symbol;
    }
  }

  if (subroutine->vars) {
    for (list_hir_var_it it = list_hir_var_begin(subroutine->vars);
         !it.end(&it); it.next(&it)) {
      hir_var    *var = it.get(&it);
      const char *name = var->id->name;

      const hir_symbol *found = hir_ctx_find_symbol(ctx, name);
      if (found) {
        hir_ctx_error_symbol_redefinition(ctx, found, var->base.span);
      } else {
        hir_symbol *symbol = hir_symbol_new(
            strdup(name), HIR_BINDING_SUBROUTINE, subroutine->binding,
            type_copy(var->type), span_copy(var->base.span));
        list_hir_symbol_push_back(ctx->hir->locals, symbol);

        hir_ctx_scope_insert_symbol(root_scope, symbol);
        var->binding = symbol;
      }
    }
  }

  hir_ctx_bind_stmt(ctx, (hir_stmt_base *)subroutine->body);

  hir_ctx_scope_free(list_hir_ctx_scope_pop_front(ctx->scopes));
}

void hir_ctx_bind_all(hir_ctx *ctx) {
  ctx->scopes = list_hir_ctx_scope_new();

  hir_ctx_scope *root_scope = hir_ctx_scope_new(HIR_CTX_SCOPE_GLOBAL);
  list_hir_ctx_scope_push_front(ctx->scopes, root_scope);

  for (list_hir_subroutine_it it =
           list_hir_subroutine_begin(ctx->hir->subroutines);
       !it.end(&it); it.next(&it)) {
    hir_subroutine *subroutine = it.get(&it);

    // add to global symbol table
    list_type *types = list_type_new();
    for (list_hir_param_it it = list_hir_param_begin(subroutine->params);
         !it.end(&it); it.next(&it)) {
      type_base *type = it.get(&it)->type;
      list_type_push_back(types, type_copy(type));
    }
    hir_symbol *symbol =
        hir_symbol_new(strdup(subroutine->id->name), HIR_BINDING_GLOBAL, NULL,
                       (type_base *)type_callable_new(
                           NULL, type_copy(subroutine->ret_type), types),
                       span_copy(subroutine->base.span));
    list_hir_symbol_push_back(ctx->hir->globals, symbol);

    // add to global scope
    const hir_symbol *found =
        hir_ctx_scope_find_symbol(root_scope, symbol->name);
    if (found) {
      hir_ctx_error_symbol_redefinition(ctx, found, symbol->decl_span);
    } else {
      hir_ctx_scope_insert_symbol(root_scope, symbol);
      subroutine->binding = symbol;
    }
  }

  for (list_hir_subroutine_it it =
           list_hir_subroutine_begin(ctx->hir->subroutines);
       !it.end(&it); it.next(&it)) {
    hir_subroutine *subroutine = it.get(&it);
    hir_ctx_bind_subroutine(ctx, subroutine);
  }

  list_hir_ctx_scope_free(ctx->scopes);
  ctx->scopes = NULL;
}
