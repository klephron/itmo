#include "lower_hir.h"
#include "compiler/exception/exception.h"
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

// BB_SEQ
typedef struct mir_bb_seq_struct {
  mir_bb *first;
  mir_bb *last;
} mir_bb_seq;

LIST_DECLARE_STATIC_INLINE(list_mir_scope_ref, mir_bb_seq, container_cmp_false,
                           container_new_move, container_delete_false);

// CTX
typedef struct mir_ctx_struct {
  hashset_mir_sym_entry *map_sym_sub;

  // handled by subroutine
  mir_subroutine        *sub_ref;
  size_t                 value_cnt;
  size_t                 bb_cnt;
  hashset_mir_sym_entry *map_sym_value;
  list_mir_scope_ref    *scope_stack;

  mir            *mir;
  list_exception *exceptions;
} mir_ctx;

static void mir_ctx_init(mir_ctx *ctx, mir *mir, list_exception *exceptions) {
  ctx->map_sym_sub = hashset_mir_sym_entry_new();

  ctx->sub_ref = NULL;
  ctx->value_cnt = 0;
  ctx->bb_cnt = 0;
  ctx->map_sym_value = NULL;
  ctx->scope_stack = NULL;

  ctx->mir = mir;
  ctx->exceptions = exceptions;
}

static void mir_ctx_deinit(mir_ctx *ctx) {
  hashset_mir_sym_entry_free(ctx->map_sym_sub);

  ctx->sub_ref = NULL;
  ctx->value_cnt = 0;
  ctx->bb_cnt = 0;
  ctx->map_sym_value = NULL;
  ctx->scope_stack = NULL;

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

static inline void mir_ctx_sym_value_create(mir_ctx *ctx) {
  ctx->map_sym_value = hashset_mir_sym_entry_new();
}

static inline void mir_ctx_sym_value_destroy(mir_ctx *ctx) {
  hashset_mir_sym_entry_free(ctx->map_sym_value);
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

static mir_bb *mir_ctx_sub_emplace_back_bb(mir_ctx *ctx) {
  mir_bb *bb = mir_bb_new(ctx->bb_cnt++, list_mir_stmt_new(), NULL, NULL, NULL,
                          list_hir_expr_ref_new());
  list_mir_bb_push_back(ctx->sub_ref->basic.bbs, bb);
  return bb;
}

// VISITOR FUNCS
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

// EXPRS
static mir_value *mir_define_expr(mir_ctx *ctx, mir_bb *bb,
                                  hir_expr_base *expr) {
  if (!expr) {
    return NULL;
  }

  switch (expr->kind) {
  case HIR_EXPR_UNKNOWN:
  case HIR_EXPR_UNARY:
  case HIR_EXPR_BINARY:
  case HIR_EXPR_LITERAL:
  case HIR_EXPR_IDENTIFIER:
  case HIR_EXPR_CALL:
  case HIR_EXPR_INDEX:
  case HIR_EXPR_BUILTIN:
    break;
  }

  return NULL;
}

// STMTS
static mir_bb_seq mir_define_stmt(mir_ctx *ctx, const hir_stmt_base *stmt);

static inline mir_bb_seq mir_bb_seq_empty() {
  return (mir_bb_seq){.first = NULL, .last = NULL};
}

static inline int mir_bb_seq_is_empty(mir_bb_seq seq) {
  return seq.first == NULL && seq.last == NULL;
}

static mir_bb_seq mir_define_stmt_if(mir_ctx *ctx, const hir_stmt_if *stmt) {
  mir_bb_seq root = {
      .first = mir_ctx_sub_emplace_back_bb(ctx),
      .last = mir_ctx_sub_emplace_back_bb(ctx),
  };

  root.first->jmp.cond_ref = mir_define_expr(ctx, root.first, stmt->cond);

  mir_bb_seq je = mir_define_stmt(ctx, stmt->je);
  root.first->jmp.je_ref = je.first;
  je.last->jmp.next_ref = root.last;

  mir_bb_seq jz = mir_define_stmt(ctx, stmt->jz);
  if (!mir_bb_seq_is_empty(jz)) {
    root.first->jmp.jz_ref = jz.first;
    jz.last->jmp.next_ref = root.last;
  }

  return root;
}

static mir_bb_seq mir_define_stmt_block(mir_ctx              *ctx,
                                        const hir_stmt_block *stmt) {
  mir_bb_seq root = {
      .first = mir_ctx_sub_emplace_back_bb(ctx),
      .last = mir_ctx_sub_emplace_back_bb(ctx),
  };

  mir_bb_seq prev = {
      .first = root.first,
      .last = root.first,
  };

  for (list_hir_stmt_it it = list_hir_stmt_begin(stmt->stmts); !END(it);
       NEXT(it)) {
    const hir_stmt_base *hir_stmt = GET(it);
    mir_bb_seq           cur = mir_define_stmt(ctx, hir_stmt);

    if (!cur.last) {
      warn("scope returned null");
      continue;
    }

    // if previous was break or return then no need to connect it with cur
    if (!prev.last->jmp.next_ref) {
      prev.last->jmp.next_ref = cur.first;
    }

    prev = cur;
  }

  if (!prev.last->jmp.next_ref) {
    prev.last->jmp.next_ref = root.last;
  }

  return root;
}

static mir_bb_seq mir_define_stmt_while(mir_ctx              *ctx,
                                        const hir_stmt_while *stmt) {
  mir_bb_seq root = {
      .first = mir_ctx_sub_emplace_back_bb(ctx),
      .last = mir_ctx_sub_emplace_back_bb(ctx),
  };

  list_mir_scope_ref_push_front(ctx->scope_stack, &root);

  root.first->jmp.cond_ref = mir_define_expr(ctx, root.first, stmt->cond);

  mir_bb_seq je = mir_define_stmt(ctx, stmt->stmt);
  root.first->jmp.je_ref = je.first;
  je.last->jmp.next_ref = root.first;

  root.first->jmp.jz_ref = root.last;

  list_mir_scope_ref_pop_front(ctx->scope_stack);

  return root;
}

static mir_bb_seq mir_define_stmt_do(mir_ctx *ctx, const hir_stmt_do *stmt) {
  mir_bb_seq root = {
      .first = NULL,
      .last = mir_ctx_sub_emplace_back_bb(ctx),
  };

  list_mir_scope_ref_push_front(ctx->scope_stack, &root);

  mir_bb_seq child_stmt = mir_define_stmt(ctx, stmt->stmt);
  mir_define_expr(ctx, child_stmt.last, stmt->cond);
  root.first = child_stmt.first;

  if (stmt->positive) {
    child_stmt.last->jmp.je_ref = root.first;
    child_stmt.last->jmp.jz_ref = root.last;
  } else {
    child_stmt.last->jmp.je_ref = root.last;
    child_stmt.last->jmp.jz_ref = root.first;
  }

  list_mir_scope_ref_pop_front(ctx->scope_stack);

  return root;
}

static mir_bb_seq mir_define_stmt_break(mir_ctx              *ctx,
                                        const hir_stmt_break *stmt) {
  mir_bb_seq root = {.first = mir_ctx_sub_emplace_back_bb(ctx)};
  root.last = root.first;

  if (list_mir_scope_ref_empty(ctx->scope_stack)) {
    mir_exception_add_error(ctx->exceptions, EXCEPTION_MIR_BREAK_UNHANDLED,
                            stmt->base.base.span, "unable to break from here");
    return root;
  }

  mir_bb_seq *scope = list_mir_scope_ref_front(ctx->scope_stack);
  root.first->jmp.next_ref = scope->last;

  return root;
}

static mir_bb_seq mir_define_stmt_expr(mir_ctx             *ctx,
                                       const hir_stmt_expr *stmt) {
  mir_bb_seq root = {.first = mir_ctx_sub_emplace_back_bb(ctx)};
  root.last = root.first;

  mir_define_expr(ctx, root.first, stmt->expr);
  list_hir_expr_ref_push_back(root.first->hir_exprs, stmt->expr);

  return root;
}

static mir_bb_seq mir_define_stmt(mir_ctx *ctx, const hir_stmt_base *stmt) {
  if (!stmt) {
    return mir_bb_seq_empty();
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
  return mir_bb_seq_empty();
}

static void mir_define_sub_basic(mir_ctx *ctx, mir_subroutine *sub_mir,
                                 const hir_subroutine *sub_hir) {
  const list_hir_var   *hir_vars = sub_hir->body->body.block.vars;
  const hir_stmt_block *hir_root = sub_hir->body->body.block.block;

  ctx->sub_ref = sub_mir;
  ctx->value_cnt = 1 + list_mir_value_size(sub_mir->basic.params);
  ctx->bb_cnt = 0;

  // add vars
  sub_mir->basic.vars = list_mir_value_new();

  for (list_hir_var_it it = list_hir_var_begin(hir_vars); !END(it); NEXT(it)) {
    const hir_var *var_hir = GET(it);

    mir_value *value =
        mir_value_new(ctx->value_cnt++, var_hir->id_ref, var_hir->type_ref);
    list_mir_value_push_back(sub_mir->basic.vars, value);
  }

  // add values to symbol scope
  mir_ctx_sym_value_create(ctx);

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

  // handle body
  sub_mir->basic.tmps = list_mir_value_new();
  sub_mir->basic.bbs = list_mir_bb_new();
  ctx->scope_stack = list_mir_scope_ref_new();

  mir_bb_seq bb_seq = mir_define_stmt_block(ctx, hir_root);

  list_mir_scope_ref_free(ctx->scope_stack);
  mir_ctx_sym_value_destroy(ctx);

  // try to add implicit return expression
  mir_bb *bb_last = bb_seq.last;
  if (!bb_last || list_mir_stmt_empty(bb_last->stmts)) {
    mir_exception_add_warning(
        ctx->exceptions, EXCEPTION_MIR_NO_RETURN, sub_mir->symbol_ref->span,
        "last block of subroutine %s is empty", sub_mir->symbol_ref->name);
  } else {
    mir_stmt  *stmt_last = list_mir_stmt_back(bb_last->stmts);
    mir_value *stmt_last_value = NULL;

    switch (stmt_last->kind) {
    case MIR_STMT_OP:
      stmt_last_value = stmt_last->op.ret;
      break;
    case MIR_STMT_CALL:
      stmt_last_value = stmt_last->call.ret;
      break;
    case MIR_STMT_MEMBER:
      stmt_last_value = stmt_last->member.ret;
      break;
    case MIR_STMT_BUILTIN:
      stmt_last_value = stmt_last->builtin.ret;
      break;
    case MIR_STMT_ASSIGN:
      stmt_last_value = stmt_last->assign.to;
      break;
    case MIR_STMT_UNKNOWN:
      break;
    }

    if (stmt_last_value) {
      list_mir_stmt_push_back(
          bb_last->stmts,
          mir_stmt_new_assign(sub_mir->basic.ret, stmt_last_value));
    } else {
      mir_exception_add_warning(
          ctx->exceptions, EXCEPTION_MIR_NO_RETURN, sub_mir->symbol_ref->span,
          "last expression of subroutine %s has no return",
          sub_mir->symbol_ref->name);
    }
  }
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
