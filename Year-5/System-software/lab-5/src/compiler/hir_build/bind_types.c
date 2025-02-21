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
  return hir_type_cmp(l->type_hir, r->type_hir);
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

static void hir_ctx_deinit(hir_ctx *ctx) { list_hir_scope_free(ctx->scopes); }

static inline type_entry *
hir_ctx_table_emplace(hir_ctx *ctx, type_base *type_base, span *span) {
  return type_table_emplace(ctx->type_table, type_base, span);
}

static hir_scope_entry *hir_ctx_scopes_find(const hir_ctx       *ctx,
                                            const hir_type_base *hir_type,
                                            hir_scope          **scope_out) {
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

// recursively tries to match type, while doint it it instantiates other types
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
    const hir_type_custom *self = (hir_type_custom *)base;

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
        hir_type_free((hir_type_base *)basic);
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
    const hir_type_array *self = (hir_type_array *)base;

    hir_scope_entry *sub_entry = hir_ctx_scopes_resolve(ctx, self->elem_type);
    if (!sub_entry) {
      return NULL;
    }

    type_array *array = type_array_new(sub_entry->type_ref->type);
    type_entry *type_ref = hir_ctx_table_emplace(ctx, (type_base *)array, NULL);

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
  debug("scope_size: %zu", scope->hashset.size);
  return GET(it);
}

static hir_scope_entry *hir_ctx_scopes_insert_global(hir_ctx         *ctx,
                                                     hir_scope_entry *entry) {
  hir_scope   *scope = list_hir_scope_back(ctx->scopes);
  hir_scope_it it = hir_scope_insert(scope, entry);
  debug("scope_size: %zu", scope->hashset.size);
  return GET(it);
}

static hir_scope_entry *hir_ctx_scopes_insert_sel(hir_ctx         *ctx,
                                                  hir_scope_entry *entry,
                                                  hir_scope       *scope) {
  UNUSED(ctx);
  hir_scope_it it = hir_scope_insert(scope, entry);
  debug("scope_size: %zu", scope->hashset.size);
  return GET(it);
}

static void hir_ctx_scopes_push(hir_ctx *ctx) {
  list_hir_scope_push_front(ctx->scopes, hir_scope_new());
  debug("scope_height: %zu", list_hir_scope_size(ctx->scopes));
}

static void hir_ctx_scopes_pop(hir_ctx *ctx) {
  hir_scope *scope = list_hir_scope_pop_front(ctx->scopes);
  ctx->scopes->list.f_delete(scope);
  debug("scope_height: %zu", list_hir_scope_size(ctx->scopes));
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

static void hir_bind_subroutine(hir_ctx *ctx, hir_subroutine *subroutine) {
  // HANDLE SUBROUTINE TYPE
  // type_entry *ret_entry = hir_bind_subroutine_match(ctx,
  // subroutine->type_ret); hir_type_free(subroutine->type_ret);

  // list_type_ref *params = list_type_ref_new();
  // for (list_hir_param_it it = list_hir_param_begin(subroutine->params);
  //      !END(it); NEXT(it)) {
  //   hir_param *param = GET(it);

  //   if (!(param->state & HIR_STATE_BIND_TYPE)) {
  //     type_entry *param_entry = hir_bind_subroutine_match(ctx,
  //     param->type_hir);

  //     hir_type_free(param->type_hir);
  //     param->type_ref = param_entry;
  //     param->state |= HIR_STATE_BIND_TYPE;
  //   }

  //   list_type_ref_push_back(params,
  //                           param->type_ref ? param->type_ref->type : NULL);
  // }

  // type_callable *callable =
  //     type_callable_new(ret_entry ? ret_entry->type : NULL, params);
  // type_entry *entry = type_table_emplace(
  //     ctx->type_table, strdup(subroutine->id_hir->name), (type_base
  //     *)callable, span_copy(subroutine->base.span));

  // subroutine->type_ref = entry;
  // subroutine->state |= HIR_STATE_BIND_TYPE;

  // HANDLE SUBROUTINE BODY
}

// add classes with templates
static type_class *hir_bind_class_1(hir_ctx *ctx, hir_class *hir_class) {
  hir_scope_entry *found;

  // ADD CLASS TO TYPE_TABLE
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

  // cleanup hir class id and span, now bound to type table
  hir_class_type = NULL;
  hir_class->id->name = NULL;
  hir_id_free(hir_class->id);
  hir_class->id = NULL;
  hir_class->base.span = NULL;

  // ADD TYPENAMES TO TYPE_TABLE
  hir_ctx_scopes_push(ctx);

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

  hir_ctx_scopes_pop(ctx);
  return class;
}

// second pass.
// 1. bind parents (check if relevant classes exist)
// 2. resolve mono types in make! expressions.
static void hir_bind_class_2(hir_ctx *ctx, hir_class *hir_class,
                             type_class *class) {
  hir_scope_entry *found;

  // BIND PARENTS
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
      hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_NULL, hir_base->span,
                    "parent with type %s not found in type table", type_s);
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

  // RESOLVE MONO make! instantiations
  // self type is already in type table.
  hir_ctx_scopes_push(ctx);

  // add typenames to scope
  for (list_type_ref_it it = list_type_ref_begin(class->typenames); !END(it);
       NEXT(it)) {
    type_typename *typename = (type_typename *)GET(it);
    hir_ctx_scopes_insert(
        ctx, hir_scope_entry_new((hir_type_base *)hir_type_custom_new(
                                     NULL, strdup(typename->id), NULL),
                                 typename->base.type_entry_ref));
  }

  for (list_hir_method_it it = list_hir_method_begin(hir_class->methods);
       !END(it); NEXT(it)) {
    hir_method *method = GET(it);

    hir_param *first = list_hir_param_front(method->subroutine->params);
    hir_type_free(first->type_hir);
    first->type_ref = class->base.type_entry_ref;
    first->state |= HIR_STATE_BIND_TYPE;

    hir_bind_subroutine(ctx, method->subroutine);
  }

  hir_ctx_scopes_pop(ctx);
}

hir_bind_types_result hir_bind_types(hir *hir) {
  hir_bind_types_result result = {
      .type_table = type_table_new(),
      .exceptions = list_exception_new(),
  };

  hir_ctx ctx = {
      .scopes = list_hir_scope_new(),
      .exceptions = result.exceptions,
      .type_table = result.type_table,
  };

  // push global scope
  hir_ctx_scopes_push(&ctx);

  hir_bind_builtin(&ctx);

  // bind classes to global scope
  list_type_ref *class_types = list_type_ref_new();

  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it)) {
    type_class *class = hir_bind_class_1(&ctx, GET(it));
    list_type_ref_push_back(class_types, (type_base *)class);
  }

  list_type_ref_it class_types_it =
      list_type_ref_begin(class_types); // to stop if step 1 failed

  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it), NEXT(class_types_it)) {

    type_class *class = (type_class *)GET(class_types_it);
    if (class) {
      hir_bind_class_2(&ctx, GET(it), class);
    }
  }
  list_type_ref_free(class_types);

  // bind subroutines to global scope
  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !END(it); NEXT(it)) {
    hir_bind_subroutine(&ctx, GET(it));
  }

  hir_ctx_scopes_pop(&ctx);
  hir_ctx_deinit(&ctx);

  return result;
}
