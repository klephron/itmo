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

void hir_ctx_bind_all(hir_ctx *ctx) {
  ctx->scopes = list_hir_ctx_scope_new();

  // add globals symbols to scope
  hir_ctx_scope *root = hir_ctx_scope_new(HIR_CTX_SCOPE_GLOBAL);
  list_hir_ctx_scope_push_back(ctx->scopes, root);

  for (list_hir_symbol_it it = list_hir_symbol_begin(ctx->hir->globals);
       !it.end(&it); it.next(&it)) {
    hir_symbol       *symbol = it.get(&it);
    const hir_symbol *found = hir_ctx_scope_find_symbol(root, symbol->name);
    if (found) {
      exception *exc = exception_new_f(
          EXCEPTION_LEVEL_ERROR, EXCEPTION_HIR,
          EXCEPTION_HIR_SYMBOL_REDEFINITION, symbol->decl_span->source,
          symbol->decl_span->line_start, symbol->decl_span->pos_start,
          exception_subtype_hir_str(EXCEPTION_HIR_SYMBOL_REDEFINITION),
          "symbol already defined %s %d:%d %d:%d, using first declaration",
          found->decl_span->source, found->decl_span->line_start,
          found->decl_span->pos_start, found->decl_span->line_end,
          found->decl_span->pos_end);
      list_exception_push_back(ctx->exceptions, exc);
    } else {
      hir_ctx_scope_insert_symbol(root, symbol);
    }
  }

  debug("after insertion");

  list_hir_ctx_scope_free(ctx->scopes);
  ctx->scopes = NULL;
}
