#include "lower_hir.h"
#include "compiler/exception/exception.h"
#include "compiler/exception/list.h"
#include "compiler/hir/str.h"
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

static inline int mir_bb_seq_is_empty(mir_bb_seq seq) {
  return seq.first == NULL && seq.last == NULL;
}

LIST_DECLARE_STATIC_INLINE(list_mir_scope_ref, mir_bb_seq, container_cmp_false,
                           container_new_move, container_delete_false);

// TYPE INDEX
static inline int container_cmp_type_ref(const void *lsv, const void *rsv) {
  const type_base *l = lsv;
  const type_base *r = rsv;
  return type_cmp(l, r);
}
static inline uint64_t container_hash_type_ref(const void *lsv) {
  const type_base *l = lsv;
  return type_hash(l);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_type_ref, type_base,
                              container_cmp_type_ref, container_new_move,
                              container_delete_false, container_hash_type_ref);

// CTX
typedef struct mir_ctx_struct {
  hashset_mir_sym_entry *map_sym_sub;
  size_t                 lit_cnt;
  hashset_type_ref      *registered_types;

  // handled by subroutine
  mir_subroutine        *sub_ref;
  size_t                 value_cnt;
  size_t                 bb_cnt;
  hashset_mir_sym_entry *map_sym_value;
  list_mir_scope_ref    *scope_stack;
  mir_bb                *bb_ref;

  mir              *mir;
  const type_table *type_table;
  list_exception   *exceptions;
} mir_ctx;

static void mir_ctx_init(mir_ctx *ctx, mir *mir, const type_table *type_table,
                         list_exception *exceptions) {
  ctx->map_sym_sub = hashset_mir_sym_entry_new();
  ctx->lit_cnt = 0;
  ctx->registered_types = hashset_type_ref_new();

  ctx->sub_ref = NULL;
  ctx->value_cnt = 0;
  ctx->bb_cnt = 0;
  ctx->map_sym_value = NULL;
  ctx->scope_stack = NULL;
  ctx->bb_ref = NULL;

  ctx->mir = mir;
  ctx->type_table = type_table;
  ctx->exceptions = exceptions;
}

static void mir_ctx_deinit(mir_ctx *ctx) {
  hashset_mir_sym_entry_free(ctx->map_sym_sub);
  ctx->lit_cnt = 0;
  hashset_type_ref_free(ctx->registered_types);

  ctx->sub_ref = NULL;
  ctx->value_cnt = 0;
  ctx->bb_cnt = 0;
  ctx->map_sym_value = NULL;
  ctx->scope_stack = NULL;
  ctx->bb_ref = NULL;

  ctx->mir = NULL;
  ctx->exceptions = NULL;
}

static void mir_ctx_setup(mir_ctx *ctx) {
  for (list_type_entry_it it = list_type_entry_begin(ctx->type_table->entries);
       !END(it); NEXT(it)) {
    hashset_type_ref_insert(ctx->registered_types, GET(it)->type);
  }
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

      return mir_subroutine_new_imported(sub_hir->id_ref, sub_hir->type_ref,
                                         lib, entry);
    }
    case HIR_SUBROUTINE_BODY_BLOCK: {
      union {
        const type_base     *base;
        const type_callable *callable;
      } type;

      type.base = sub_hir->type_ref->type;

      if (type.base->kind != TYPE_CALLABLE) {
        mir_exception_add_error(ctx->exceptions, EXCEPTION_MIR_UNEXPECTED_TYPE,
                                sub_hir->id_ref->span,
                                "expected type %s, got %s %p",
                                type_enum_str(TYPE_CALLABLE),
                                type_enum_str(type.base->kind), sub_hir);
        return NULL;
      }

      ctx->value_cnt = 0;

      mir_value *ret_value = mir_value_new(
          ctx->value_cnt++, NULL, type.callable->ret_ref->type_entry_ref);

      list_mir_value *params = list_mir_value_new();
      for (list_hir_param_it it = list_hir_param_begin(sub_hir->params);
           !END(it); NEXT(it)) {
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
static mir_value *mir_define_expr(mir_ctx *ctx, const hir_expr_base *expr);

static list_mir_value_ref *mir_define_mir_value_args(mir_value *first_ref,
                                                     ...) {
  list_mir_value_ref *args = list_mir_value_ref_new();

  if (first_ref == NULL) {
    return args;
  }
  list_mir_value_ref_push_back(args, first_ref);

  va_list ap;
  va_start(ap, first_ref);

  while (1) {
    mir_value *arg = va_arg(ap, mir_value *);
    if (arg == NULL) {
      break;
    }
    list_mir_value_ref_push_back(args, arg);
  }

  va_end(ap);
  return args;
}

// returns reference
static mir_value *mir_define_mir_value_tmp(mir_ctx          *ctx,
                                           const type_entry *type_ref) {
  mir_value *res = mir_value_new(ctx->value_cnt++, NULL, type_ref);
  list_mir_value_push_back(ctx->sub_ref->basic.tmps, res);
  return res;
}

// returns reference
static mir_stmt *mir_define_mir_stmt(mir_ctx *ctx, mir_stmt *stmt) {
  list_mir_stmt_push_back(ctx->bb_ref->stmts, stmt);
  return stmt;
}

// returns reference
// type is not resolved
static mir_lit *mir_define_mir_lit(mir_ctx *ctx, const hir_expr_lit *expr) {
  const type_entry *entry_ref = expr->lit->type_ref;

  // validate
  int ok = 1;

  if (!entry_ref) {
    error("type_ref shouldn't be null");
    ok = 0;
  }

  if (ok) {
    switch (entry_ref->type->kind) {
      case TYPE_PRIMITIVE:
        break;
      case TYPE_ARRAY:
      case TYPE_CALLABLE:
      case TYPE_CLASS:
      case TYPE_TYPENAME:
      case TYPE_MONO:
      case TYPE_UNKNOWN: {
        char *type_s = type_str(entry_ref->type);
        error("unsupported lit type %s %p", type_s, expr);
        ok = 0;
      } break;
    }
  }

  if (!ok) {
    return NULL;
  }

  // build
  mir_lit_value value;
  hir_lit_u     hir_value = expr->lit->value;

  const type_primitive *type = (typeof(type))entry_ref->type;
  switch (type->type) {
    case TYPE_PRIMITIVE_BOOL:
      value.v_bool = hir_value.v_bool;
      break;
    case TYPE_PRIMITIVE_BYTE:
      value.v_byte = (uint8_t)hir_value.v_ulong;
      break;
    case TYPE_PRIMITIVE_INT:
      value.v_int = (int32_t)hir_value.v_long;
      break;
    case TYPE_PRIMITIVE_UINT:
      value.v_uint = (uint32_t)hir_value.v_ulong;
      break;
    case TYPE_PRIMITIVE_LONG:
      value.v_long = (int64_t)hir_value.v_long;
      break;
    case TYPE_PRIMITIVE_ULONG:
      value.v_ulong = (uint64_t)hir_value.v_ulong;
      break;
    case TYPE_PRIMITIVE_CHAR:
      value.v_char = (uint8_t)hir_value.v_char[0];
      break;
    case TYPE_PRIMITIVE_STRING:
      value.v_str = (uint8_t *)strdup(hir_value.v_str);
      break;
    case TYPE_PRIMITIVE_VOID:
      value.v_bool = 0;
      break;
    case TYPE_PRIMITIVE_UNKNOWN:
    default:
      error("type %d is unresolved", type->type);
      value.v_bool = 0;
  }

  mir_lit *lit = mir_lit_new(ctx->lit_cnt++, entry_ref, value);
  list_mir_lit_push_back(ctx->mir->literals, lit);
  return lit;
}

// static mir_lit *mir_define_mir_lit_unregistered(mir_ctx *ctx, type_base
// *type,
//                                                 mir_lit_value value) {
//   hashset_type_ref_it it = hashset_type_ref_find(ctx->registered_types,
//   type); if (END(it)) {
//     char *type_s = type_str(type);
//     error("literal type %s is unresolved", type_s);
//     free(type_s);
//     return NULL;
//   }
//   mir_lit *lit = mir_lit_new(ctx->lit_cnt++, GET(it)->type_entry_ref, value);
//   list_mir_lit_push_back(ctx->mir->literals, lit);
//   return lit;
// }

static mir_value *mir_define_expr_unary_generic(mir_ctx              *ctx,
                                                const hir_expr_unary *expr,
                                                mir_stmt_op_enum      op) {
  mir_value *first = mir_define_expr(ctx, expr->first);
  mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
  mir_define_mir_stmt(
      ctx, mir_stmt_new_op(op, ret, mir_define_mir_value_args(first, NULL)));
  return ret;
}

static mir_value *mir_define_expr_unary(mir_ctx              *ctx,
                                        const hir_expr_unary *expr) {
  if (!expr) {
    return NULL;
  }

  // validate
  int ok = 1;

  switch (expr->op) {
    case HIR_EXPR_UNARY_PLUS:
    case HIR_EXPR_UNARY_MINUS:
    case HIR_EXPR_UNARY_LOGICAL_NOT:
    case HIR_EXPR_UNARY_BITWISE_NOT:
      if (!expr->first) {
        ok = 0;
      }
      break;

    case HIR_EXPR_UNARY_INC:
    case HIR_EXPR_UNARY_DEC: {
      if (!expr->first) {
        ok = 0;
        break;
      }
      if (expr->first->kind != HIR_EXPR_IDENTIFIER) {
        ok = 0;
        mir_exception_add_error(ctx->exceptions,
                                EXCEPTION_MIR_UNEXPECTED_RVALUE,
                                expr->base.base.span, "expected identifier");
        break;
      }
      break;
    }

    case HIR_EXPR_UNARY_UNKNOWN:
    default:
      ok = 0;
      break;
  }

  if (!ok) {
    debug("hir expr %p is invalid", expr);
    return NULL;
  }

  // emit
  switch (expr->op) {
    case HIR_EXPR_UNARY_PLUS: {
      return mir_define_expr_unary_generic(ctx, expr, MIR_STMT_OP_UNARY_PLUS);
    }
    case HIR_EXPR_UNARY_MINUS: {
      return mir_define_expr_unary_generic(ctx, expr, MIR_STMT_OP_UNARY_MINUS);
    }
    case HIR_EXPR_UNARY_LOGICAL_NOT: {
      return mir_define_expr_unary_generic(ctx, expr,
                                           MIR_STMT_OP_UNARY_LOGICAL_NOT);
    }
    case HIR_EXPR_UNARY_BITWISE_NOT: {
      return mir_define_expr_unary_generic(ctx, expr,
                                           MIR_STMT_OP_UNARY_BITWISE_NOT);
    }
      // NOTE: can't replace with +1 because strict typing and
      // can't guess type of identifier for sure, else may replace with ADD_ONE
    case HIR_EXPR_UNARY_INC: {
      return mir_define_expr_unary_generic(ctx, expr, MIR_STMT_OP_UNARY_INC);
    }
    case HIR_EXPR_UNARY_DEC: {
      return mir_define_expr_unary_generic(ctx, expr, MIR_STMT_OP_UNARY_DEC);
    }
    case HIR_EXPR_UNARY_UNKNOWN:
      break;
  }

  error("unhandled hir unary expr %s(%d) %p", hir_expr_unary_enum_str(expr->op),
        expr->op, expr);
  return NULL;
}

static mir_value *mir_define_expr_binary_generic(mir_ctx               *ctx,
                                                 const hir_expr_binary *expr,
                                                 mir_stmt_op_enum       op) {
  mir_value *first = mir_define_expr(ctx, expr->first);
  mir_value *second = mir_define_expr(ctx, expr->second);
  mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
  mir_define_mir_stmt(
      ctx,
      mir_stmt_new_op(op, ret, mir_define_mir_value_args(first, second, NULL)));
  return ret;
}

static mir_value *mir_define_expr_binary(mir_ctx               *ctx,
                                         const hir_expr_binary *expr) {
  int ok = 1;

  if (!expr) {
    ok = 0;
  }

  switch (expr->op) {
    case HIR_EXPR_BINARY_MEMBER: {
      const type_entry *type_ref;

      if (expr->second->kind != HIR_EXPR_LITERAL ||
          !(type_ref = ((hir_expr_lit *)expr->second)->lit->type_ref) ||
          type_ref->type->kind != TYPE_PRIMITIVE ||
          ((type_primitive *)type_ref->type)->type != TYPE_PRIMITIVE_STRING) {

        mir_exception_add_error(
            ctx->exceptions, EXCEPTION_MIR_UNEXPECTED_MEMBER,
            expr->base.base.span,
            "member access only allowed with literal string");
        ok = 0;
      }

      break;
    }

    case HIR_EXPR_BINARY_ASSIGN:
    case HIR_EXPR_BINARY_LOGICAL_OR:
    case HIR_EXPR_BINARY_LOGICAL_AND:
    case HIR_EXPR_BINARY_BITWISE_OR:
    case HIR_EXPR_BINARY_BITWISE_XOR:
    case HIR_EXPR_BINARY_BITWISE_AND:
    case HIR_EXPR_BINARY_EQUALS:
    case HIR_EXPR_BINARY_NOT_EQUALS:
    case HIR_EXPR_BINARY_LESS:
    case HIR_EXPR_BINARY_LESS_EQUALS:
    case HIR_EXPR_BINARY_GREATER:
    case HIR_EXPR_BINARY_GREATER_EQUALS:
    case HIR_EXPR_BINARY_BITWISE_SHIFT_LEFT:
    case HIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT:
    case HIR_EXPR_BINARY_ADD:
    case HIR_EXPR_BINARY_SUB:
    case HIR_EXPR_BINARY_MUL:
    case HIR_EXPR_BINARY_DIV:
    case HIR_EXPR_BINARY_REM:
    case HIR_EXPR_BINARY_UNKNOWN:
      break;
  }

  if (!ok) {
    return NULL;
  }

  // build
  switch (expr->op) {
    case HIR_EXPR_BINARY_ASSIGN: {
      mir_value *from = mir_define_expr(ctx, expr->second);
      mir_value *to = mir_define_expr(ctx, expr->first);
      mir_define_mir_stmt(ctx, mir_stmt_new_assign_value(to, from));
      return to;
    }
    case HIR_EXPR_BINARY_LOGICAL_OR: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_LOGICAL_OR);
    }
    case HIR_EXPR_BINARY_LOGICAL_AND: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_LOGICAL_AND);
    }
    case HIR_EXPR_BINARY_BITWISE_OR: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_BITWISE_OR);
    }
    case HIR_EXPR_BINARY_BITWISE_XOR: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_BITWISE_XOR);
    }
    case HIR_EXPR_BINARY_BITWISE_AND: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_BITWISE_AND);
    }
    case HIR_EXPR_BINARY_EQUALS: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_EQUALS);
    }
    case HIR_EXPR_BINARY_NOT_EQUALS: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_NOT_EQUALS);
    }
    case HIR_EXPR_BINARY_LESS: {
      return mir_define_expr_binary_generic(ctx, expr, MIR_STMT_OP_BINARY_LESS);
    }
    case HIR_EXPR_BINARY_LESS_EQUALS: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_LESS_EQUALS);
    }
    case HIR_EXPR_BINARY_GREATER: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_GREATER);
    }
    case HIR_EXPR_BINARY_GREATER_EQUALS: {
      return mir_define_expr_binary_generic(ctx, expr,
                                            MIR_STMT_OP_BINARY_GREATER_EQUALS);
    }
    case HIR_EXPR_BINARY_BITWISE_SHIFT_LEFT: {
      return mir_define_expr_binary_generic(
          ctx, expr, MIR_STMT_OP_BINARY_BITWISE_SHIFT_LEFT);
    }
    case HIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT: {
      return mir_define_expr_binary_generic(
          ctx, expr, MIR_STMT_OP_BINARY_BITWISE_SHIFT_RIGHT);
    }
    case HIR_EXPR_BINARY_ADD: {
      return mir_define_expr_binary_generic(ctx, expr, MIR_STMT_OP_BINARY_ADD);
    }
    case HIR_EXPR_BINARY_SUB: {
      return mir_define_expr_binary_generic(ctx, expr, MIR_STMT_OP_BINARY_SUB);
    }
    case HIR_EXPR_BINARY_MUL: {
      return mir_define_expr_binary_generic(ctx, expr, MIR_STMT_OP_BINARY_MUL);
    }
    case HIR_EXPR_BINARY_DIV: {
      return mir_define_expr_binary_generic(ctx, expr, MIR_STMT_OP_BINARY_DIV);
    }
    case HIR_EXPR_BINARY_REM: {
      return mir_define_expr_binary_generic(ctx, expr, MIR_STMT_OP_BINARY_REM);
    }
    case HIR_EXPR_BINARY_MEMBER: {
      mir_value *obj = mir_define_expr(ctx, expr->first);
      mir_lit   *member = mir_define_mir_lit(ctx, (hir_expr_lit *)expr->second);
      mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
      mir_define_mir_stmt(ctx, mir_stmt_new_member(ret, obj, member));
      return ret;
    }
    case HIR_EXPR_BINARY_UNKNOWN:
      break;
  }

  error("unhandled hir binary expr %s(%d) %p",
        hir_expr_binary_enum_str(expr->op), expr->op, expr);
  return NULL;
}

static mir_value *mir_define_expr_lit(mir_ctx *ctx, const hir_expr_lit *expr) {
  mir_value *ret = mir_define_mir_value_tmp(ctx, expr->lit->type_ref);
  mir_lit   *lit = mir_define_mir_lit(ctx, expr);
  mir_define_mir_stmt(ctx, mir_stmt_new_assign_lit(ret, lit));
  return ret;
}

// if propagated here than need to return value
static mir_value *mir_define_expr_id(mir_ctx *ctx, const hir_expr_id *expr) {

  // check if in variables
  mir_value *value = mir_ctx_sym_value_find(ctx, expr->id_ref);
  if (!value) {
    mir_exception_add_error(
        ctx->exceptions, EXCEPTION_MIR_UNEXPECTED_IDENTIFIER,
        expr->id_ref->span,
        "no variable %s found, subroutines in this context are not allowed",
        expr->id_ref->name);
    return NULL;
  }

  return value;
}

static mir_value *mir_define_expr_call(mir_ctx             *ctx,
                                       const hir_expr_call *expr) {

  list_mir_value_ref *args = list_mir_value_ref_new();
  for (list_hir_expr_it it = list_hir_expr_begin(expr->args); !END(it);
       NEXT(it)) {
    const hir_expr_base *arg = GET(it);
    list_mir_value_ref_push_back(args, mir_define_expr(ctx, arg));
  }

  switch (expr->callee->kind) {
    case HIR_EXPR_IDENTIFIER: {
      // if in subroutines then direct, else indirect
      const hir_expr_id *callee = (typeof(callee))expr->callee;

      mir_subroutine *subroutine = mir_ctx_sym_sub_find(ctx, callee->id_ref);
      if (subroutine) {
        mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
        mir_define_mir_stmt(ctx, mir_stmt_new_call(ret, subroutine, args));
        return ret;
      }

      mir_value *indirect = mir_define_expr_id(ctx, callee);
      if (indirect) {
        list_mir_value_ref_push_front(args, indirect);
        mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
        mir_define_mir_stmt(
            ctx, mir_stmt_new_op(MIR_STMT_OP_CALL_INDIRECT, ret, args));
        return ret;
      }

      error("shouldn't propagate here, identifier must be resoled");
      return NULL;
    }

    case HIR_EXPR_UNARY:
    case HIR_EXPR_BINARY:
    case HIR_EXPR_LITERAL:
    case HIR_EXPR_CALL:
    case HIR_EXPR_INDEX:
    case HIR_EXPR_BUILTIN: {
      mir_value *indirect = mir_define_expr(ctx, expr->callee);
      if (indirect) {
        list_mir_value_ref_push_front(args, indirect);
        mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
        mir_define_mir_stmt(
            ctx, mir_stmt_new_op(MIR_STMT_OP_CALL_INDIRECT, ret, args));
        return ret;
      }
    }

    case HIR_EXPR_UNKNOWN:
      break;
  }

  list_mir_value_ref_free(args);
  error("unexpected callee kind %d %p", expr->callee->kind, expr);
  return NULL;
}

static mir_value *mir_define_expr_index(mir_ctx              *ctx,
                                        const hir_expr_index *expr) {
  list_mir_value_ref *args = list_mir_value_ref_new();
  for (list_hir_expr_it it = list_hir_expr_begin(expr->args); !END(it);
       NEXT(it)) {
    const hir_expr_base *arg = GET(it);
    list_mir_value_ref_push_back(args, mir_define_expr(ctx, arg));
  }

  mir_value *indirect = mir_define_expr(ctx, expr->indexed);

  if (indirect) {
    list_mir_value_ref_push_front(args, indirect);
    mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
    mir_define_mir_stmt(ctx,
                        mir_stmt_new_op(MIR_STMT_OP_INDEX_INDIRECT, ret, args));
    return ret;
  }

  list_mir_value_ref_free(args);
  return NULL;
}

static mir_value *mir_define_expr_builtin(mir_ctx                *ctx,
                                          const hir_expr_builtin *expr) {
  if (!expr) {
    return NULL;
  }

  list_mir_value_ref *args = list_mir_value_ref_new();
  for (list_hir_expr_it it = list_hir_expr_begin(expr->args); !END(it);
       NEXT(it)) {
    const hir_expr_base *arg = GET(it);
    list_mir_value_ref_push_back(args, mir_define_expr(ctx, arg));
  }

  switch (expr->kind) {
    case HIR_EXPR_BUILTIN_CAST: {
      mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
      mir_define_mir_stmt(ctx, mir_stmt_new_builtin(MIR_STMT_BUILTIN_CAST, ret,
                                                    expr->base.type_ref, args));
      return ret;
    }
    case HIR_EXPR_BUILTIN_MAKE: {
      mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
      mir_define_mir_stmt(ctx, mir_stmt_new_builtin(MIR_STMT_BUILTIN_MAKE, ret,
                                                    expr->base.type_ref, args));
      return ret;
    }
    case HIR_EXPR_BUILTIN_PRINT: {
      mir_value *ret = mir_define_mir_value_tmp(ctx, NULL);
      mir_define_mir_stmt(ctx, mir_stmt_new_builtin(MIR_STMT_BUILTIN_PRINT, ret,
                                                    expr->base.type_ref, args));
      return ret;
    }
    case HIR_EXPR_BUILTIN_UNKNOWN:
      break;
  }

  error("unexpected builtin expr kind");
  return NULL;
}

static mir_value *mir_define_expr(mir_ctx *ctx, const hir_expr_base *expr) {
  if (!expr) {
    return NULL;
  }

  switch (expr->kind) {
    case HIR_EXPR_UNARY:
      return mir_define_expr_unary(ctx, (hir_expr_unary *)expr);
    case HIR_EXPR_BINARY:
      return mir_define_expr_binary(ctx, (hir_expr_binary *)expr);
    case HIR_EXPR_LITERAL:
      return mir_define_expr_lit(ctx, (hir_expr_lit *)expr);
    case HIR_EXPR_IDENTIFIER:
      return mir_define_expr_id(ctx, (hir_expr_id *)expr);
    case HIR_EXPR_CALL:
      return mir_define_expr_call(ctx, (hir_expr_call *)expr);
    case HIR_EXPR_INDEX:
      return mir_define_expr_index(ctx, (hir_expr_index *)expr);
    case HIR_EXPR_BUILTIN:
      return mir_define_expr_builtin(ctx, (hir_expr_builtin *)expr);
    case HIR_EXPR_UNKNOWN:
      break;
  }

  error("unexpected expr kind %d %p", expr->kind, expr);
  return NULL;
}

static mir_value *mir_define_expr_tree(mir_ctx *ctx, mir_bb *bb,
                                       const hir_expr_base *expr) {
  ctx->bb_ref = bb;
  mir_value *value_ref = mir_define_expr(ctx, expr);
  ctx->bb_ref = NULL;
  return value_ref;
}

// STMTS
static mir_bb_seq mir_define_stmt(mir_ctx *ctx, const hir_stmt_base *stmt);

static inline mir_bb_seq mir_define_bb_seq_empty() {
  return (mir_bb_seq){.first = NULL, .last = NULL};
}

static mir_bb_seq mir_define_stmt_if(mir_ctx *ctx, const hir_stmt_if *stmt) {
  mir_bb_seq root = {
      .first = mir_ctx_sub_emplace_back_bb(ctx),
      .last = mir_ctx_sub_emplace_back_bb(ctx),
  };

  root.first->jmp.cond_ref = mir_define_expr_tree(ctx, root.first, stmt->cond);

  mir_bb_seq je = mir_define_stmt(ctx, stmt->je);
  root.first->jmp.je_ref = je.first;
  je.last->jmp.next_ref = root.last;

  mir_bb_seq jz = mir_define_stmt(ctx, stmt->jz);
  if (!mir_bb_seq_is_empty(jz)) {
    root.first->jmp.jz_ref = jz.first;
    jz.last->jmp.next_ref = root.last;
  } else {
    root.first->jmp.jz_ref = root.last;
  }

  return root;
}

static mir_bb_seq mir_define_stmt_block(mir_ctx              *ctx,
                                        const hir_stmt_block *stmt) {
  mir_bb_seq root = {
      .first = mir_ctx_sub_emplace_back_bb(ctx),
      .last = NULL,
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

  root.last = prev.last;
  return root;
}

static mir_bb_seq mir_define_stmt_while(mir_ctx              *ctx,
                                        const hir_stmt_while *stmt) {
  mir_bb_seq root = {
      .first = mir_ctx_sub_emplace_back_bb(ctx),
      .last = mir_ctx_sub_emplace_back_bb(ctx),
  };

  list_mir_scope_ref_push_front(ctx->scope_stack, &root);

  root.first->jmp.cond_ref = mir_define_expr_tree(ctx, root.first, stmt->cond);

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
  mir_define_expr_tree(ctx, child_stmt.last, stmt->cond);
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
    mir_exception_add_error(ctx->exceptions, EXCEPTION_MIR_UNEXPECTED_BREAK,
                            stmt->base.base.span, "unable to break from here");
    return root;
  }

  mir_bb_seq *scope = list_mir_scope_ref_front(ctx->scope_stack);
  root.first->jmp.next_ref = scope->last;

  return root;
}

static mir_bb_seq mir_define_bb_expr(mir_ctx *ctx, const hir_stmt_expr *stmt) {
  mir_bb_seq root = {.first = mir_ctx_sub_emplace_back_bb(ctx)};
  root.last = root.first;

  mir_define_expr_tree(ctx, root.first, stmt->expr);
  list_hir_expr_ref_push_back(root.first->hir_exprs, stmt->expr);

  return root;
}

static mir_bb_seq mir_define_stmt(mir_ctx *ctx, const hir_stmt_base *stmt) {
  if (!stmt) {
    return mir_define_bb_seq_empty();
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
      return mir_define_bb_expr(ctx, (hir_stmt_expr *)stmt);
    case HIR_STMT_UNKNOWN:
      break;
  }

  error("unexpectes stmt kind %d %p", stmt->kind, stmt);
  return mir_define_bb_seq_empty();
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
      case MIR_STMT_ASSIGN_VALUE:
        stmt_last_value = stmt_last->value.to;
        break;
      case MIR_STMT_ASSIGN_LIT:
        stmt_last_value = stmt_last->lit.to;
        break;
      case MIR_STMT_UNKNOWN:
        break;
    }

    if (stmt_last_value) {
      list_mir_stmt_push_back(
          bb_last->stmts,
          mir_stmt_new_assign_value(sub_mir->basic.ret, stmt_last_value));
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

mir_lower_hir_result mir_lower_hir(const hir        *hir,
                                   const type_table *type_table) {
  mir_lower_hir_result result = {
      .mir = mir_new(),
      .exceptions = list_exception_new(),
  };

  mir_ctx ctx;
  mir_ctx_init(&ctx, result.mir, type_table, result.exceptions);
  mir_ctx_setup(&ctx);

  mir_declare_subs(&ctx, hir);
  mir_define_subs(&ctx, hir);

  mir_ctx_deinit(&ctx);

  return result;
}
