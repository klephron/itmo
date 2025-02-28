#include "lower_hir.h"
#include "compiler/exception/list.h"
#include "compiler/mir/mir.h"
#include "compiler/mir_build/exception.h"
#include "compiler/type_table/str.h"
#include "util/hashset.h"
#include "util/log.h"
#include "util/macro.h"
#include <string.h>

// - resolve global subroutines (not methods)
// - resolve mir classes with dummy mir_subroutines
// - map[hir_subroutine] -> mir_subroutine
// - instantiate mir_subroutines that are actually used

// SYM_ENTRY
typedef struct mir_sym_entry_struct {
  const symbol_entry *symbol_ref;
  void               *entry_ref;
} mir_sym_entry;

static mir_sym_entry *mir_sym_entry_new(const symbol_entry *symbol_ref,
                                        void               *entry_ref) {
  mir_sym_entry *self = MALLOC(mir_sym_entry);
  self->symbol_ref = symbol_ref;
  self->entry_ref = entry_ref;
  return self;
}

static void mir_sym_entry_free(mir_sym_entry *self) {
  if (self) {
    free(self);
  }
}

static inline int container_cmp_mir_sym_entry(const void *lsv,
                                              const void *rsv) {
  const mir_sym_entry *l = lsv;
  const mir_sym_entry *r = rsv;
  return container_cmp_ptr(l->symbol_ref, r->symbol_ref);
}
static inline uint64_t container_hash_mir_sym_entry(const void *lsv) {
  const mir_sym_entry *l = lsv;
  return container_hash_ptr(l->symbol_ref);
}
static inline void container_delete_mir_sym_entry(void *self) {
  mir_sym_entry_free(self);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_mir_sym_entry, mir_sym_entry,
                              container_cmp_mir_sym_entry, container_new_move,
                              container_delete_mir_sym_entry,
                              container_hash_mir_sym_entry);

// CTX
typedef struct mir_ctx_struct {
  hashset_mir_sym_entry *map_sym_sub;

  size_t                 value_cnt;
  hashset_mir_sym_entry *map_sym_value;
  mir_subroutine        *sub_ref;
  mir_bb                *sub_last_bb;

  mir            *mir;
  list_exception *exceptions;
} mir_ctx;

static void mir_ctx_init(mir_ctx *ctx, mir *mir, list_exception *exceptions) {
  ctx->map_sym_sub = hashset_mir_sym_entry_new();

  ctx->value_cnt = 0;
  ctx->map_sym_value = NULL;
  ctx->sub_ref = NULL;
  ctx->sub_last_bb = NULL;

  ctx->mir = mir;
  ctx->exceptions = exceptions;
}

static void mir_ctx_deinit(mir_ctx *ctx) {
  hashset_mir_sym_entry_free(ctx->map_sym_sub);

  ctx->value_cnt = 0;
  if (ctx->map_sym_value) {
    hashset_mir_sym_entry_free(ctx->map_sym_value);
    ctx->map_sym_value = NULL;
  }
  ctx->sub_ref = NULL;

  ctx->mir = NULL;
  ctx->exceptions = NULL;
}

static void mir_ctx_sym_sub_emplace(mir_ctx            *ctx,
                                    const symbol_entry *symbol_ref,
                                    mir_subroutine     *sub) {
  hashset_mir_sym_entry_insert(ctx->map_sym_sub,
                               mir_sym_entry_new(symbol_ref, sub));
}

static mir_subroutine *mir_ctx_sym_sub_find(mir_ctx            *ctx,
                                            const symbol_entry *symbol_ref) {
  hashset_mir_sym_entry_it it = hashset_mir_sym_entry_find(
      ctx->map_sym_sub, &(mir_sym_entry){.symbol_ref = symbol_ref});
  if (!END(it)) {
    return GET(it)->entry_ref;
  }
  return NULL;
}

static void mir_ctx_sym_value_recreate(mir_ctx *ctx) {
  if (ctx->map_sym_value) {
    hashset_mir_sym_entry_free(ctx->map_sym_value);
  }
  ctx->map_sym_value = hashset_mir_sym_entry_new();
}

static void mir_ctx_sym_value_emplace(mir_ctx            *ctx,
                                      const symbol_entry *symbol_ref,
                                      mir_value          *value) {
  hashset_mir_sym_entry_insert(ctx->map_sym_value,
                               mir_sym_entry_new(symbol_ref, value));
}

static mir_value *mir_ctx_sym_value_find(mir_ctx            *ctx,
                                         const symbol_entry *symbol_ref) {
  hashset_mir_sym_entry_it it = hashset_mir_sym_entry_find(
      ctx->map_sym_value, &(mir_sym_entry){.symbol_ref = symbol_ref});
  if (!END(it)) {
    return GET(it)->entry_ref;
  }
  return NULL;
}

// VISITOR FUNCS
static mir_bb *mir_define_stmt_if(mir_ctx *ctx, const hir_stmt_if *stmt) {
  return NULL;
}

static mir_bb *mir_define_stmt_block(mir_ctx *ctx, const hir_stmt_block *stmt) {
  return NULL;
}

static mir_bb *mir_define_stmt_while(mir_ctx *ctx, const hir_stmt_while *stmt) {
  return NULL;
}

static mir_bb *mir_define_stmt_do(mir_ctx *ctx, const hir_stmt_do *stmt) {
  return NULL;
}

static mir_bb *mir_define_stmt_break(mir_ctx *ctx, const hir_stmt_break *stmt) {
  return NULL;
}

static mir_bb *mir_define_stmt_expr(mir_ctx *ctx, const hir_stmt_expr *stmt) {
  return NULL;
}

static mir_bb *mir_define_stmt(mir_ctx *ctx, const hir_stmt_base *stmt) {
  if (!stmt) {
    return NULL;
  }

  switch (stmt->kind) {
  case HIR_STMT_IF:
    return mir_define_stmt_if(ctx, (hir_stmt_if *)stmt);
  case HIR_STMT_BLOCK:
    return mir_define_stmt_block(ctx, (hir_stmt_block *)stmt);
  case HIR_STMT_WHILE:
    return mir_define_stmt_while(ctx, (hir_stmt_while *)stmt);
  case HIR_STMT_DO:
    return mir_define_stmt_do(ctx, (hir_stmt_do *)stmt);
  case HIR_STMT_BREAK:
    return mir_define_stmt_break(ctx, (hir_stmt_break *)stmt);
  case HIR_STMT_EXPR:
    return mir_define_stmt_expr(ctx, (hir_stmt_expr *)stmt);
  case HIR_STMT_UNKNOWN:
    break;
  }

  error("unexpectes stmt kind %d %p", stmt->kind, stmt);
  return NULL;
}

static void mir_define_sub_basic(mir_ctx *ctx, mir_subroutine *sub_mir,
                                 const hir_subroutine *sub_hir) {
  const list_hir_var   *hir_vars = sub_hir->body->body.block.vars;
  const hir_stmt_block *hir_root = sub_hir->body->body.block.block;

  ctx->value_cnt = 1 + list_mir_value_size(sub_mir->basic.params);

  // add vars
  sub_mir->basic.vars = list_mir_value_new();
  for (list_hir_var_it it = list_hir_var_begin(hir_vars); !END(it); NEXT(it)) {
    const hir_var *var_hir = GET(it);

    mir_value *value =
        mir_value_new(ctx->value_cnt++, var_hir->id_ref, var_hir->type_ref);
    list_mir_value_push_back(sub_mir->basic.vars, value);
  }

  // add values to symbol scope
  mir_ctx_sym_value_recreate(ctx);

  for (list_mir_value_it it = list_mir_value_begin(sub_mir->basic.params);
       !END(it); NEXT(it)) {
    mir_value *param = GET(it);
    mir_ctx_sym_value_emplace(ctx, param->symbol_ref, param);
  }
  for (list_mir_value_it it = list_mir_value_begin(sub_mir->basic.vars);
       !END(it); NEXT(it)) {
    mir_value *param = GET(it);
    mir_ctx_sym_value_emplace(ctx, param->symbol_ref, param);
  }

  sub_mir->basic.tmps = list_mir_value_new();
  sub_mir->basic.bbs = list_mir_bb_new();
  ctx->sub_ref = sub_mir;
  mir_define_stmt_block(ctx, hir_root);
}

static void mir_define_subs(mir_ctx *ctx, const hir *hir) {
  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !END(it); NEXT(it)) {
    const hir_subroutine *sub_hir = GET(it);

    mir_subroutine *sub_mir;
    if (!(sub_mir = mir_ctx_sym_sub_find(ctx, sub_hir->id_ref))) {
      error("subroutine %s not found %p", sub_hir->id_ref->name, sub_hir);
      continue;
    }

    if (sub_mir->kind == MIR_SUBROUTINE_BASIC) {
      mir_define_sub_basic(ctx, sub_mir, sub_hir);
    }
  }
}

static mir_subroutine *mir_declare_sub(mir_ctx              *ctx,
                                       const hir_subroutine *sub_hir) {
  if (!sub_hir->body) {
    return mir_subroutine_new_external(sub_hir->id_ref, sub_hir->type_ref);
  }

  switch (sub_hir->body->kind) {
  case HIR_SUBROUTINE_BODY_IMPORT: {
    const hir_lit *lib_lit = sub_hir->body->body.import.lib;
    const hir_lit *entry_lit = sub_hir->body->body.import.entry;

    char *lib = lib_lit ? strdup(lib_lit->value.v_str) : NULL;
    char *entry = entry_lit ? strdup(entry_lit->value.v_str) : NULL;

    return mir_subroutine_new_imported(sub_hir->id_ref, sub_hir->type_ref, lib,
                                       entry);
  }
  case HIR_SUBROUTINE_BODY_BLOCK: {
    union {
      const type_base     *base;
      const type_callable *callable;
    } type;

    type.base = sub_hir->type_ref->type;

    if (type.base->kind != TYPE_CALLABLE) {
      mir_exception_add_error(
          ctx->exceptions, EXCEPTION_MIR_UNEXPECTED_TYPE, sub_hir->id_ref->span,
          "expected type %s, got %s %p", type_enum_str(TYPE_CALLABLE),
          type_enum_str(type.base->kind), sub_hir);
      return NULL;
    }

    ctx->value_cnt = 0;

    mir_value *ret_value = mir_value_new(
        ctx->value_cnt++, NULL, type.callable->ret_ref->type_entry_ref);

    list_mir_value *params = list_mir_value_new();
    for (list_hir_param_it it = list_hir_param_begin(sub_hir->params); !END(it);
         NEXT(it)) {
      const hir_param *param_hir = GET(it);

      mir_value *param = mir_value_new(ctx->value_cnt++, param_hir->id_ref,
                                       param_hir->type_ref);
      list_mir_value_push_back(params, param);
    }

    return mir_subroutine_new_basic(sub_hir->id_ref, sub_hir->type_ref,
                                    ret_value, params, NULL, NULL, NULL);
  }
  case HIR_SUBROUTINE_BODY_UNKNOWN:
    break;
  }
  error("unexpected subroutine body kind %d %p", sub_hir->body->kind, sub_hir);
  return NULL;
}

static void mir_declare_subs(mir_ctx *ctx, const hir *hir) {
  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !END(it); NEXT(it)) {
    const hir_subroutine *sub_hir = GET(it);

    mir_subroutine *sub = mir_declare_sub(ctx, sub_hir);
    if (!sub) {
      continue;
    }

    // add to index (to then parse bodies and resolve references)
    mir_ctx_sym_sub_emplace(ctx, sub->symbol_ref, sub);

    switch (sub->kind) {
    case MIR_SUBROUTINE_BASIC:
      list_mir_subroutine_push_back(ctx->mir->global_subs, sub);
      break;
    case MIR_SUBROUTINE_EXTERNAL:
      list_mir_subroutine_push_back(ctx->mir->external_subs, sub);
      break;
    case MIR_SUBROUTINE_IMPORTED:
      list_mir_subroutine_push_back(ctx->mir->imported_subs, sub);
      break;
    case MIR_SUBROUTINE_UNKNOWN:
    default:
      break;
    }
  }
}

mir_lower_hir_result mir_lower_hir(const hir *hir) {
  mir_lower_hir_result result = {
      .mir = mir_new(),
      .exceptions = list_exception_new(),
  };

  mir_ctx ctx;
  mir_ctx_init(&ctx, result.mir, result.exceptions);

  mir_declare_subs(&ctx, hir);
  mir_define_subs(&ctx, hir);

  mir_ctx_deinit(&ctx);

  return result;
}
