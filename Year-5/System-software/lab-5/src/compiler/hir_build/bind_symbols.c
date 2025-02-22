#include "bind_symbols.h"

#include "compiler/hir_build/exception.h"
#include "util/hashset.h"
#include "util/log.h"
#include "util/macro.h"

// hir_scope
static inline int container_cmp_hir_scope_entry(const void *lsv,
                                                const void *rsv) {
  const symbol_entry *l = lsv;
  const symbol_entry *r = rsv;
  return container_cmp_chars(l->name, r->name);
}
static inline uint64_t container_hash_hir_scope_entry(const void *lsv) {
  const symbol_entry *l = lsv;
  return container_hash_chars(l->name);
}
HASHSET_DECLARE_STATIC_INLINE(hir_scope, symbol_entry,
                              container_cmp_hir_scope_entry, container_new_move,
                              container_delete_false,
                              container_hash_hir_scope_entry);

static inline void container_delete_scope(void *data) { hir_scope_free(data); }
LIST_DECLARE_STATIC_INLINE(list_hir_scope, hir_scope, container_cmp_false,
                           container_new_move, container_delete_scope);

typedef struct hir_ctx_struct {
  list_hir_scope *scopes;
  list_exception *exceptions;
  symbol_table   *symbol_table;
} hir_ctx;

static void hir_ctx_deinit(hir_ctx *ctx) { list_hir_scope_free(ctx->scopes); }

static symbol_entry *hir_ctx_scopes_insert(hir_ctx *ctx, symbol_entry *entry) {
  hir_scope   *scope = list_hir_scope_front(ctx->scopes);
  hir_scope_it it = hir_scope_insert(scope, entry);
  debug("scope_size: %zu", scope->hashset.size);
  return GET(it);
}

static void hir_ctx_scopes_push(hir_ctx *ctx) {
  list_hir_scope_push_front(ctx->scopes, hir_scope_new());
  debug("scopes_height: %zu", list_hir_scope_size(ctx->scopes));
}

static void hir_ctx_scopes_pop(hir_ctx *ctx) {
  hir_scope *scope = list_hir_scope_pop_front(ctx->scopes);
  ctx->scopes->list.f_delete(scope);
  debug("scopes_height: %zu", list_hir_scope_size(ctx->scopes));
}

static inline symbol_entry *
hir_ctx_table_emplace(hir_ctx *ctx, char *id, type_entry *type, span *span) {
  return symbol_table_emplace(ctx->symbol_table, id, type, span);
}

static void hir_ctx_error(hir_ctx *ctx, exception_subtype_hir subtype,
                          const span *span, const char *format, ...) {

  va_list args;
  va_start(args, format);
  hir_exception_add_error_v(ctx->exceptions, subtype, span, format, args);
  va_end(args);
}

// visitor function

static void hir_bind_subroutine(hir_ctx *ctx, hir_subroutine *hir_subroutine) {}

static void hir_bind_class(hir_ctx *ctx, hir_class *hir_class) {
  hir_ctx_scopes_push(ctx);

  for (list_hir_var_it it = list_hir_var_begin(hir_class->fields); !END(it);
       NEXT(it)) {
    hir_var *var = GET(it);
  }

  hir_ctx_scopes_pop(ctx);
}

hir_bind_symbols_result hir_bind_symbols(hir *hir) {
  hir_bind_symbols_result result = {
      .symbol_table = symbol_table_new(),
      .exceptions = list_exception_new(),
  };

  hir_ctx ctx = {
      .scopes = list_hir_scope_new(),
      .exceptions = result.exceptions,
      .symbol_table = result.symbol_table,
  };

  // push global scope
  hir_ctx_scopes_push(&ctx);

  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it)) {
    hir_class *class = GET(it);
    hir_bind_class(&ctx, class);
  }

  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !END(it); NEXT(it)) {
    hir_subroutine *sub = GET(it);
    hir_bind_subroutine(&ctx, sub);
  }

  hir_ctx_scopes_pop(&ctx);
  hir_ctx_deinit(&ctx);

  return result;
}
