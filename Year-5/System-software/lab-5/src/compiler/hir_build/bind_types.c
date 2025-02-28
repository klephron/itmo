#include "bind_types.h"
#include "compiler/hir/str.h"
#include "compiler/hir_build/exception.h"
#include "compiler/type_table/str.h"
#include "util/hashset.h"
#include "util/log.h"
#include "util/macro.h"
#include <string.h>

// scope_entry
typedef struct hir_scope_entry_struct {
  hir_type_base *type_hir;
  type_entry    *type_ref;
} hir_scope_entry;

static hir_scope_entry *hir_scope_entry_new(hir_type_base *type_hir,
                                            type_entry    *type_ref) {
  hir_scope_entry *self = MALLOC(hir_scope_entry);
  self->type_hir = type_hir;
  self->type_ref = type_ref;
  return self;
}

static void hir_scope_entry_free(hir_scope_entry *self) {
  if (self) {
    hir_type_free(self->type_hir);
    self->type_ref = NULL;
    free(self);
  }
}

// hir_scope
static inline int container_cmp_hir_scope_entry(const void *lsv,
                                                const void *rsv) {
  const hir_scope_entry *l = lsv;
  const hir_scope_entry *r = rsv;
  int                    c = hir_type_cmp(l->type_hir, r->type_hir);

  // char *t1 = hir_type_str(l->type_hir);
  // char *t2 = hir_type_str(r->type_hir);

  // debug("comparing %s %s = %d", t1, t2, c);
  // if (t1) {
  //   free(t1);
  // }
  // if (t2) {
  //   free(t2);
  // }

  return c;
}
static inline uint64_t container_hash_hir_scope_entry(const void *lsv) {
  const hir_scope_entry *l = lsv;
  return hir_type_hash(l->type_hir);
}
static inline void container_delete_hir_scope_entry(void *lsv) {
  return hir_scope_entry_free((hir_scope_entry *)lsv);
}
HASHSET_DECLARE_STATIC_INLINE(hir_scope, hir_scope_entry,
                              container_cmp_hir_scope_entry, container_new_move,
                              container_delete_hir_scope_entry,
                              container_hash_hir_scope_entry);

// list_hir_scope
static inline void container_delete_scope(void *data) { hir_scope_free(data); }
LIST_DECLARE_STATIC_INLINE(list_hir_scope, hir_scope, container_cmp_false,
                           container_new_move, container_delete_scope);

// ctx
typedef struct hir_ctx_struct {
  list_hir_scope *scopes;
  list_exception *exceptions;
  type_table     *type_table;
} hir_ctx;

static hir_scope_entry *hir_ctx_scopes_insert(hir_ctx         *ctx,
                                              hir_scope_entry *entry);
static hir_scope_entry *hir_ctx_scopes_insert_global(hir_ctx         *ctx,
                                                     hir_scope_entry *entry);
static hir_scope_entry *hir_ctx_scopes_insert_sel(hir_ctx         *ctx,
                                                  hir_scope_entry *entry,
                                                  hir_scope       *scope);
static void             hir_ctx_scopes_push(hir_ctx *ctx);
static void             hir_ctx_scopes_pop(hir_ctx *ctx);

static void hir_ctx_init(hir_ctx *ctx, type_table *type_table,
                         list_exception *exceptions) {
  ctx->scopes = list_hir_scope_new();
  ctx->type_table = type_table;
  ctx->exceptions = exceptions;
}

static void hir_ctx_deinit(hir_ctx *ctx) { list_hir_scope_free(ctx->scopes); }

static inline type_entry *
hir_ctx_table_emplace(hir_ctx *ctx, type_base *type_base, span *span) {
  return type_table_emplace(ctx->type_table, type_base, span);
}

static hir_scope_entry *hir_ctx_scopes_find(const hir_ctx       *ctx,
                                            const hir_type_base *hir_type,
                                            hir_scope          **scope_out) {
  // debug("current scopes: ");
  // for (list_hir_scope_it it = list_hir_scope_begin(ctx->scopes); !END(it);
  //      NEXT(it)) {
  //   for (hir_scope_it sit = hir_scope_begin(GET(it)); !END(sit); NEXT(sit)) {
  //     hir_scope_entry *entry = GET(sit);

  //     char *hir_type_s = hir_type_str(entry->type_hir);
  //     char *type_s = type_str(entry->type_ref->type);

  //     debug("  %s -> %s", hir_type_s, type_s);
  //     free(hir_type_s);
  //     free(type_s);
  //   }
  // }
  // debug("----------");

  for (list_hir_scope_it it = list_hir_scope_begin(ctx->scopes); !END(it);
       NEXT(it)) {
    hir_scope   *scope = GET(it);
    hir_scope_it scope_it = hir_scope_find(
        scope, &(hir_scope_entry){.type_hir = (hir_type_base *)hir_type});

    if (!END(scope_it)) {
      if (scope_out) {
        *scope_out = scope;
      }
      return GET(scope_it);
    }
  }
  return NULL;
}

// recursively tries to match type
static hir_scope_entry *hir_ctx_scopes_resolve(hir_ctx             *ctx,
                                               const hir_type_base *base) {
  if (!base) {
    return NULL;
  }

  hir_scope_entry *entry = hir_ctx_scopes_find(ctx, base, NULL);

  if (entry) {
    return entry;
  }

  switch (base->type) {
  case HIR_TYPE_CUSTOM: {
    // try to mono instantiate type type
    const hir_type_custom *self = (typeof(self))base;

    if (!self->templates || !list_hir_type_size(self->templates)) {
      return NULL;
    }

    hir_type_custom *basic =
        hir_type_custom_new(NULL, strdup(self->name), list_hir_type_new());

    for (size_t i = 0, sz = list_hir_type_size(self->templates); i < sz; ++i) {
      list_hir_type_push_back(basic->templates, NULL);
    }

    // can't be resolved
    hir_scope *scope;
    hir_scope_entry *template =
        hir_ctx_scopes_find(ctx, (hir_type_base *)basic, &scope);

    hir_type_free((hir_type_base *)basic);

    if (!template) {
      return NULL;
    }

    // create new entry in scope of found entry
    type_mono *mono =
        type_mono_new(template->type_ref->type, list_type_ref_new());

    // resolve mono-ed types
    for (list_hir_type_it it = list_hir_type_begin(self->templates); !END(it);
         NEXT(it)) {
      hir_type_base   *type = GET(it);
      hir_scope_entry *sub_entry = hir_ctx_scopes_resolve(ctx, type);

      if (!sub_entry) {
        type_free((type_base *)mono);
        return NULL;
      }

      list_type_ref_push_back(mono->types, sub_entry->type_ref->type);
    }

    type_entry *type_ref = hir_ctx_table_emplace(
        ctx, (type_base *)mono, span_copy(template->type_ref->span));

    entry = hir_ctx_scopes_insert_sel(
        ctx,
        hir_scope_entry_new(hir_type_copy((hir_type_base *)self), type_ref),
        scope);

  } break;

  case HIR_TYPE_ARRAY: {
    const hir_type_array *self = (typeof(self))base;

    hir_scope_entry *sub_entry = hir_ctx_scopes_resolve(ctx, self->elem_type);
    if (!sub_entry) {
      return NULL;
    }

    type_array *array = type_array_new(sub_entry->type_ref->type);
    type_entry *type_ref = hir_ctx_table_emplace(ctx, (type_base *)array,
                                                 span_copy(self->base.span));

    entry = hir_ctx_scopes_insert_global(
        ctx,
        hir_scope_entry_new(hir_type_copy((hir_type_base *)self), type_ref));

  } break;
  case HIR_TYPE_BOOL:
  case HIR_TYPE_BYTE:
  case HIR_TYPE_INT:
  case HIR_TYPE_UINT:
  case HIR_TYPE_LONG:
  case HIR_TYPE_ULONG:
  case HIR_TYPE_CHAR:
  case HIR_TYPE_STRING:
  case HIR_TYPE_VOID:
    error("type %s should be already be in scope, but not found",
          hir_type_enum_str(base->type));
    break;
  default:
    error("unexpected type kind %d", base->type);
    break;
  }
  return entry;
}

static hir_scope_entry *hir_ctx_scopes_insert(hir_ctx         *ctx,
                                              hir_scope_entry *entry) {
  hir_scope   *scope = list_hir_scope_front(ctx->scopes);
  hir_scope_it it = hir_scope_insert(scope, entry);
  // debug("scope_size: %zu", scope->hashset.size);
  return GET(it);
}

static hir_scope_entry *hir_ctx_scopes_insert_global(hir_ctx         *ctx,
                                                     hir_scope_entry *entry) {
  hir_scope   *scope = list_hir_scope_back(ctx->scopes);
  hir_scope_it it = hir_scope_insert(scope, entry);
  // debug("scope_size: %zu", scope->hashset.size);
  return GET(it);
}

static hir_scope_entry *hir_ctx_scopes_insert_sel(hir_ctx         *ctx,
                                                  hir_scope_entry *entry,
                                                  hir_scope       *scope) {
  UNUSED(ctx);
  hir_scope_it it = hir_scope_insert(scope, entry);
  // debug("scope_size: %zu", scope->hashset.size);
  return GET(it);
}

static void hir_ctx_scopes_push(hir_ctx *ctx) {
  list_hir_scope_push_front(ctx->scopes, hir_scope_new());
  // debug("scopes_height: %zu", list_hir_scope_size(ctx->scopes));
}

static void hir_ctx_scopes_pop(hir_ctx *ctx) {
  hir_scope *scope = list_hir_scope_pop_front(ctx->scopes);
  ctx->scopes->list.f_delete(scope);
  // debug("scopes_height: %zu", list_hir_scope_size(ctx->scopes));
}

static void hir_ctx_error(hir_ctx *ctx, exception_subtype_hir subtype,
                          const span *span, const char *format, ...) {

  va_list args;
  va_start(args, format);
  hir_exception_add_error_v(ctx->exceptions, subtype, span, format, args);
  va_end(args);
}

// visitor functions

// first pass. bind class name and typenames, adding them to global scope
static void hir_bind_builtin(hir_ctx *ctx) {
  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_BOOL),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_BOOL),
                   NULL)));

  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_BYTE),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_BYTE),
                   NULL)));

  hir_ctx_scopes_insert_global(
      ctx,
      hir_scope_entry_new(
          hir_type_base_new(NULL, HIR_TYPE_INT),
          hir_ctx_table_emplace(
              ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_INT), NULL)));

  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_UINT),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_UINT),
                   NULL)));

  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_LONG),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_LONG),
                   NULL)));

  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_ULONG),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_ULONG),
                   NULL)));

  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_CHAR),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_CHAR),
                   NULL)));

  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_STRING),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_STRING),
                   NULL)));

  hir_ctx_scopes_insert_global(
      ctx, hir_scope_entry_new(
               hir_type_base_new(NULL, HIR_TYPE_VOID),
               hir_ctx_table_emplace(
                   ctx, (type_base *)type_primitive_new(TYPE_PRIMITIVE_VOID),
                   NULL)));
}

static void hir_bind_lit(hir_ctx *ctx, hir_lit *lit) {
  if (!lit || (lit->state & HIR_STATE_BIND_TYPE)) {
    return;
  }

  if (lit->type_hir) {
    hir_scope_entry *entry = hir_ctx_scopes_resolve(ctx, lit->type_hir);
    if (!entry) {
      char *type_s = hir_type_str(lit->type_hir);

      hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNRESOLVED, lit->base.span,
                    "lit with type %s can't be resolved", type_s);

      free(type_s);
      return;
    }
    hir_type_free(lit->type_hir);
    lit->type_ref = entry->type_ref;
  }
  lit->state |= HIR_STATE_BIND_TYPE;
}

static void hir_bind_subroutine_signature(hir_ctx        *ctx,
                                          hir_subroutine *subroutine) {
  hir_scope_entry *ret_entry =
      hir_ctx_scopes_resolve(ctx, subroutine->type_ret);

  if (!ret_entry) {
    char *type_s = hir_type_str(subroutine->type_ret);
    hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNRESOLVED, subroutine->base.span,
                  "return type %s can't be resolved", type_s);
    free(type_s);
    return;
  }

  list_type_ref *params = list_type_ref_new();
  for (list_hir_param_it it = list_hir_param_begin(subroutine->params);
       !END(it); NEXT(it)) {
    hir_param *param = GET(it);

    if (!(param->state & HIR_STATE_BIND_TYPE)) {
      if (param->type_hir) {
        hir_scope_entry *param_entry =
            hir_ctx_scopes_resolve(ctx, param->type_hir);

        if (!param_entry) {
          char *type_s = hir_type_str(param->type_hir);
          hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNRESOLVED,
                        subroutine->base.span,
                        "param with type %s can't be resolved", type_s);
          free(type_s);

          list_type_ref_free(params);
          return; // can't parse subroutine type signature
        }

        hir_type_free(param->type_hir);
        param->type_ref = param_entry->type_ref;
      }
      param->state |= HIR_STATE_BIND_TYPE;
    }

    list_type_ref_push_back(params,
                            param->type_ref ? param->type_ref->type : NULL);
  }

  type_callable *callable =
      type_callable_new(ret_entry ? ret_entry->type_ref->type : NULL, params);

  // add to type table, but not to scopes
  // callable types can't be declared using grammar
  type_entry *callable_entry = hir_ctx_table_emplace(
      ctx, (type_base *)callable, span_copy(subroutine->base.span));

  hir_type_free(subroutine->type_ret);
  subroutine->type_ref = callable_entry;
  subroutine->state |= HIR_STATE_BIND_TYPE;
}

static void hir_bind_vars(hir_ctx *ctx, list_hir_var *vars) {
  for (list_hir_var_it it = list_hir_var_begin(vars); !END(it); NEXT(it)) {
    hir_var *var = GET(it);

    hir_scope_entry *entry = hir_ctx_scopes_resolve(ctx, var->type_hir);
    if (!entry) {
      const char *var_s = (var->state & HIR_STATE_BIND_SYMBOL)
                              ? var->id_ref->name
                              : var->id_hir->name;
      char       *type_s = hir_type_str(var->type_hir);

      hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNRESOLVED, var->base.span,
                    "var %s type %s can't be resolved", var_s, type_s);

      free(type_s);
      continue;
    }

    hir_type_free(var->type_hir);
    var->type_ref = entry->type_ref;
    var->state |= HIR_STATE_BIND_TYPE;
  }
}

static void hir_bind_expr(hir_ctx *ctx, hir_expr_base *base) {
  if (!base) {
    return;
  }

  switch (base->kind) {
  case HIR_EXPR_UNARY: {
    hir_expr_unary *self = (typeof(self))base;
    hir_bind_expr(ctx, self->first);
    break;
  }
  case HIR_EXPR_BINARY: {
    hir_expr_binary *self = (typeof(self))base;
    hir_bind_expr(ctx, self->first);
    hir_bind_expr(ctx, self->second);
    break;
  }
  case HIR_EXPR_LITERAL: {
    hir_expr_lit *self = (typeof(self))base;
    hir_bind_lit(ctx, self->lit);
    break;
  }
  case HIR_EXPR_IDENTIFIER: {
    break;
  }
  case HIR_EXPR_CALL: {
    hir_expr_call *self = (typeof(self))base;
    hir_bind_expr(ctx, self->callee);
    for (list_hir_expr_it it = list_hir_expr_begin(self->args); !END(it);
         NEXT(it)) {
      hir_bind_expr(ctx, GET(it));
    }
    break;
  }
  case HIR_EXPR_INDEX: {
    hir_expr_index *self = (typeof(self))base;
    hir_bind_expr(ctx, self->indexed);
    for (list_hir_expr_it it = list_hir_expr_begin(self->args); !END(it);
         NEXT(it)) {
      hir_bind_expr(ctx, GET(it));
    }
    break;
  }
  case HIR_EXPR_BUILTIN: {
    hir_expr_builtin *self = (typeof(self))base;
    for (list_hir_expr_it it = list_hir_expr_begin(self->args); !END(it);
         NEXT(it)) {
      hir_bind_expr(ctx, GET(it));
    }
    break;
  }
  case HIR_EXPR_UNKNOWN:
    error("unexpected expr kind %d %p", base->kind, base);
    break;
  }

  if (!(base->state & HIR_STATE_BIND_TYPE)) {
    if (base->type_hir) {
      hir_scope_entry *entry = hir_ctx_scopes_resolve(ctx, base->type_hir);
      if (!entry) {
        char *type_s = hir_type_str(base->type_hir);

        hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNRESOLVED, base->base.span,
                      "expr type %s can't be resolved", type_s);

        free(type_s);
        return;
      }

      hir_type_free(base->type_hir);
      base->type_ref = entry->type_ref;
    }
    base->state |= HIR_STATE_BIND_TYPE;
  }
}

static void hir_bind_stmt(hir_ctx *ctx, hir_stmt_base *base) {
  if (!base) {
    return;
  }
  switch (base->kind) {
  case HIR_STMT_IF: {
    hir_stmt_if *self = (typeof(self))base;
    hir_bind_expr(ctx, self->cond);
    hir_bind_stmt(ctx, self->je);
    hir_bind_stmt(ctx, self->jz);
    return;
  };
  case HIR_STMT_BLOCK: {
    hir_stmt_block *self = (typeof(self))base;
    for (list_hir_stmt_it it = list_hir_stmt_begin(self->stmts); !END(it);
         NEXT(it)) {
      hir_bind_stmt(ctx, GET(it));
    }
    return;
  };
  case HIR_STMT_WHILE: {
    hir_stmt_while *self = (typeof(self))base;
    hir_bind_expr(ctx, self->cond);
    hir_bind_stmt(ctx, self->stmt);
    return;
  };
  case HIR_STMT_DO: {
    hir_stmt_do *self = (typeof(self))base;
    hir_bind_stmt(ctx, self->stmt);
    hir_bind_expr(ctx, self->cond);
    return;
  };
  case HIR_STMT_BREAK: {
    return;
  };
  case HIR_STMT_EXPR: {
    hir_stmt_expr *self = (typeof(self))base;
    hir_bind_expr(ctx, self->expr);
    return;
  };
  case HIR_STMT_UNKNOWN:
    break;
  }
  error("unexpected stmt kind %d %p", base->kind, base);
}

static void hir_bind_stmt_block(hir_ctx *ctx, hir_stmt_block *block) {
  if (!block) {
    return;
  }
  hir_bind_stmt(ctx, (hir_stmt_base *)block);
}

static void hir_bind_subroutine_body(hir_ctx *ctx, hir_subroutine_body *body) {
  if (body) {
    switch (body->kind) {
    case HIR_SUBROUTINE_BODY_UNKNOWN:
      break;
    case HIR_SUBROUTINE_BODY_IMPORT:
      hir_bind_lit(ctx, body->body.import.lib);
      hir_bind_lit(ctx, body->body.import.entry);
      break;
    case HIR_SUBROUTINE_BODY_BLOCK:
      hir_bind_vars(ctx, body->body.block.vars);
      hir_bind_stmt_block(ctx, body->body.block.block);
      break;
    default:
      error("unexpected block subroutine kind %d", body->kind);
    }
  }
}

static void hir_bind_subroutine(hir_ctx *ctx, hir_subroutine *subroutine) {
  hir_bind_subroutine_signature(ctx, subroutine);
  hir_bind_subroutine_body(ctx, subroutine->body);
}

// adds class to type table
static type_class *hir_bind_class_id(hir_ctx *ctx, hir_class *hir_class) {
  hir_scope_entry *found;

  hir_type_custom *hir_class_type = hir_type_custom_new(
      NULL, strdup(hir_class->id->name), list_hir_type_new());

  for (size_t i = 0, sz = list_hir_id_size(hir_class->typenames); i < sz; ++i) {
    list_hir_type_push_back(hir_class_type->templates, NULL);
  }

  if ((found =
           hir_ctx_scopes_find(ctx, (hir_type_base *)hir_class_type, NULL))) {
    char *type_s = hir_type_str((hir_type_base *)hir_class_type);
    char *found_s = type_str(found->type_ref->type);
    hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_REDEFINITION, hir_class->base.span,
                  "type name collision %s with %s", type_s, type_s);
    free(type_s);
    free(found_s);
    hir_type_free((hir_type_base *)hir_class_type);
    return NULL;
  }

  type_class *class = type_class_new(hir_class->id->name, list_type_ref_new(),
                                     list_type_ref_new());
  type_entry *class_entry =
      hir_ctx_table_emplace(ctx, (type_base *)class, hir_class->base.span);

  hir_ctx_scopes_insert(
      ctx, hir_scope_entry_new((hir_type_base *)hir_class_type, class_entry));
  hir_class_type = NULL;

  // cleanup hir class id and span, now bound to type table
  hir_class->id->name = NULL;
  hir_id_free(hir_class->id);
  hir_class->id = NULL;
  hir_class->base.span = NULL;

  return class;
}

// adds typenames to type table
static void hir_bind_class_typenames(hir_ctx *ctx, hir_class *hir_class,
                                     type_class *class) {
  hir_scope_entry *found;

  for (list_hir_id_it it = list_hir_id_begin(hir_class->typenames); !END(it);
       NEXT(it)) {
    hir_id *hir_typename = GET(it);

    hir_type_custom *hir_typename_type =
        hir_type_custom_new(NULL, strdup(hir_typename->name), NULL);

    if ((found = hir_ctx_scopes_find(ctx, (hir_type_base *)hir_typename_type,
                                     NULL))) {
      char *type_s = hir_type_str((hir_type_base *)hir_typename_type);
      char *found_s = type_str(found->type_ref->type);
      hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_REDEFINITION,
                    hir_typename->base.span, "type name collision %s with %s",
                    type_s, found_s);
      free(type_s);
      free(found_s);
      hir_type_free((hir_type_base *)hir_typename_type);
      break;
    }

    type_typename *typename =
        type_typename_new(hir_typename->name, (type_base *)class);
    type_entry *typename_entry = hir_ctx_table_emplace(
        ctx, (type_base *)typename, hir_typename->base.span);
    hir_ctx_scopes_insert(
        ctx, hir_scope_entry_new((hir_type_base *)hir_typename_type,
                                 typename_entry));

    list_type_ref_push_back(class->typenames, (type_base *)typename);

    hir_typename->name = NULL;
    hir_typename->base.span = NULL;
  }
  list_hir_id_free(hir_class->typenames);
  hir_class->typenames = NULL;
}

// second pass.
// 1. bind parents (check if relevant classes exist)
// 2. resolve mono types in make! expressions.
static void hir_bind_class_parents(hir_ctx *ctx, hir_class *hir_class,
                                   type_class *class) {
  hir_scope_entry *found;

  for (list_hir_type_it it = list_hir_type_begin(hir_class->parents); !END(it);
       NEXT(it)) {

    hir_type_base *hir_base = GET(it);
    if (hir_base->type != HIR_TYPE_CUSTOM) {
      hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNEXPECTED, hir_base->span,
                    "expected parent with type %s, got %s",
                    hir_type_enum_str(HIR_TYPE_CUSTOM),
                    hir_type_enum_str(hir_base->type));
      break;
    }

    hir_type_custom *hir_parent = (hir_type_custom *)hir_base;

    if (!(found = hir_ctx_scopes_resolve(ctx, (hir_type_base *)hir_parent))) {
      char *type_s = hir_type_str((hir_type_base *)hir_parent);
      hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNRESOLVED, hir_base->span,
                    "parent with type %s can't be resolved", type_s);
      free(type_s);
      break;
    }

    if (!(found->type_ref->type->kind == TYPE_CLASS ||
          found->type_ref->type->kind == TYPE_MONO)) {
      hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNEXPECTED, hir_base->span,
                    "in type table expected parent type %s or %s, got %s",
                    type_enum_str(TYPE_CLASS), type_enum_str(TYPE_MONO),
                    type_enum_str(found->type_ref->type->kind));
      break;
    }

    list_type_ref_push_back(class->parents, found->type_ref->type);
  }

  list_hir_type_free(hir_class->parents);
  hir_class->parents = NULL;
}

static void hir_bind_class_fields(hir_ctx *ctx, hir_class *hir_class,
                                  type_class *class) {
  UNUSED(class);

  for (list_hir_var_it it = list_hir_var_begin(hir_class->fields); !END(it);
       NEXT(it)) {
    hir_var *var = GET(it);

    if (!(var->state & HIR_STATE_BIND_TYPE)) {
      if (var->type_hir) {
        hir_scope_entry *var_entry = hir_ctx_scopes_resolve(ctx, var->type_hir);

        if (!var_entry) {
          char *type_s = hir_type_str(var->type_hir);
          hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_UNRESOLVED, var->base.span,
                        "var with type %s can't be resolved", type_s);
          free(type_s);

          continue;
        }

        hir_type_free(var->type_hir);
        var->type_ref = var_entry->type_ref;
      }
      var->state |= HIR_STATE_BIND_TYPE;
    }
  }
}

static void hir_bind_class_methods(hir_ctx *ctx, hir_class *hir_class,
                                   type_class *class) {
  UNUSED(class);

  for (list_hir_method_it it = list_hir_method_begin(hir_class->methods);
       !END(it); NEXT(it)) {
    hir_method *method = GET(it);

    // init first param as mono of current class
    hir_param *first = list_hir_param_front(method->subroutine->params);

    type_entry *first_type =
        hir_ctx_scopes_resolve(ctx, first->type_hir)->type_ref;

    hir_type_free(first->type_hir);
    first->type_ref = first_type;
    first->state |= HIR_STATE_BIND_TYPE;

    hir_bind_subroutine(ctx, method->subroutine);
  }
}

static void hir_bind_classes(hir_ctx *ctx, hir *hir) {
  list_type_ref *classes = list_type_ref_new();

  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it)) {
    type_class *class = hir_bind_class_id(ctx, GET(it));

    if (class) {
      hir_ctx_scopes_push(ctx);
      hir_bind_class_typenames(ctx, GET(it), class);
      hir_ctx_scopes_pop(ctx);
    }

    list_type_ref_push_back(classes, (type_base *)class);
  }

  list_type_ref_it classes_it = list_type_ref_begin(classes);

  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it), NEXT(classes_it)) {
    hir_class *hir_class = GET(it);
    type_class *class = (typeof(class))GET(classes_it);

    if (class) {
      hir_ctx_scopes_push(ctx);

      // add typenames to scope
      for (list_type_ref_it it = list_type_ref_begin(class->typenames);
           !END(it); NEXT(it)) {
        type_typename *typename = (typeof(typename))GET(it);
        hir_ctx_scopes_insert(
            ctx, hir_scope_entry_new((hir_type_base *)hir_type_custom_new(
                                         NULL, strdup(typename->id), NULL),
                                     typename->base.type_entry_ref));
      }

      hir_bind_class_parents(ctx, hir_class, class);
      hir_bind_class_fields(ctx, hir_class, class);
      hir_bind_class_methods(ctx, hir_class, class);

      hir_class->type_ref = class->base.type_entry_ref;
      hir_class->state |= HIR_STATE_BIND_TYPE;

      hir_ctx_scopes_pop(ctx);
    }
  }
  list_type_ref_free(classes);
}

hir_bind_types_result hir_bind_types(hir *hir) {
  hir_bind_types_result result = {
      .type_table = type_table_new(),
      .exceptions = list_exception_new(),
  };

  hir_ctx ctx;
  hir_ctx_init(&ctx, result.type_table, result.exceptions);

  // push global scope
  hir_ctx_scopes_push(&ctx);

  hir_bind_builtin(&ctx);
  hir_bind_classes(&ctx, hir);

  // bind subroutines to global scope
  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !END(it); NEXT(it)) {
    hir_bind_subroutine(&ctx, GET(it));
  }

  hir_ctx_scopes_pop(&ctx);
  hir_ctx_deinit(&ctx);

  return result;
}
