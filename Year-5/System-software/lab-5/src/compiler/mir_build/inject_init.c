#include "inject_init.h"
#include "util/hashset.h"
#include "util/macro.h"

// TYPE_SUB
typedef struct mir_type_sub_struct {
  const type_entry *type_ref;
  mir_subroutine   *sub_ref;
} mir_type_sub;

static mir_type_sub *mir_type_sub_new(const type_entry *type_ref,
                                      mir_subroutine   *sub_ref) {
  mir_type_sub *self = MALLOC(mir_type_sub);
  self->type_ref     = type_ref;
  self->sub_ref      = sub_ref;
  return self;
}

static void mir_type_sub_free(mir_type_sub *self) {
  if (self) {
    free(self);
  }
}

static inline int container_cmp_mir_type_sub(const void *lsv, const void *rsv) {
  const mir_type_sub *l = lsv;
  const mir_type_sub *r = rsv;
  return container_cmp_ptr(l->type_ref, r->type_ref);
}
static inline uint64_t container_hash_mir_type_sub(const void *lsv) {
  const mir_type_sub *l = lsv;
  return container_hash_ptr(l->type_ref);
}
static inline void container_delete_mir_type_sub(void *self) {
  mir_type_sub_free(self);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_mir_type_sub, mir_type_sub,
                              container_cmp_mir_type_sub, container_new_move,
                              container_delete_mir_type_sub,
                              container_hash_mir_type_sub);

// CTX
typedef struct mir_ctx_struct {
  hashset_mir_type_sub *map_type_sub;

  mir            *mir;
  symbol_table   *symbol_table;
  list_exception *exceptions;
} mir_ctx;

static void mir_ctx_init(mir_ctx *ctx, mir *mir, symbol_table *symbol_table,
                         list_exception *exceptions) {
  ctx->map_type_sub = hashset_mir_type_sub_new();

  ctx->mir          = mir;
  ctx->symbol_table = symbol_table;
  ctx->exceptions   = exceptions;
}

static void mir_ctx_deinit(mir_ctx *ctx) {
  hashset_mir_type_sub_free(ctx->map_type_sub);

  ctx->mir          = NULL;
  ctx->symbol_table = NULL;
  ctx->exceptions   = NULL;
}

// replace make builtin stmts for classes with initializer subroutines
static void mir_inject_init_subroutine(mir_subroutine *sub) {}

mir_inject_init_result mir_inject_init(mir *mir) {
  mir_inject_init_result result = {
      .symbol_table = symbol_table_new(),
      .exceptions   = list_exception_new(),
  };

  mir_ctx ctx;
  mir_ctx_init(&ctx, mir, result.symbol_table, result.exceptions);

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->global_subs);
       !END(it); NEXT(it)) {
    mir_inject_init_subroutine(GET(it));
  }

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->methods);
       !END(it); NEXT(it)) {
    mir_inject_init_subroutine(GET(it));
  }

  mir_ctx_deinit(&ctx);

  return result;
}
