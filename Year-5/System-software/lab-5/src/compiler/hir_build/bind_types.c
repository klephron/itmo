#include "bind_types.h"
#include "compiler/hir_build/exception.h"
#include "util/log.h"
#include "util/macro.h"
#include <string.h>

// CONTEXT
HASHSET_DECLARE_STATIC_INLINE(hashset_type_entry_ref, type_entry,
                              container_cmp_type_entry, container_new_move,
                              container_delete_false,
                              container_hash_type_entry);

static inline void container_delete_scope_types(void *data) {
  hashset_type_entry_ref_free(data);
}

LIST_DECLARE_STATIC_INLINE(list_hir_scope, hashset_type_entry_ref,
                           container_cmp_false, container_new_move,
                           container_delete_scope_types);

typedef struct hir_ctx_struct {
  list_hir_scope *scopes;
  list_exception *exceptions;
  type_table     *type_table;
} hir_ctx;

static void hir_ctx_deinit(hir_ctx *ctx) { list_hir_scope_free(ctx->scopes); }

static type_entry *hir_ctx_scopes_find(const hir_ctx *ctx, const char *id) {
  for (list_hir_scope_it it = list_hir_scope_begin(ctx->scopes); !END(it);
       NEXT(it)) {
    hashset_type_entry_ref   *scope = GET(it);
    hashset_type_entry_ref_it scope_it =
        hashset_type_entry_ref_find(scope, &(type_entry){.name = (char *)id});

    if (!END(scope_it)) {
      return GET(scope_it);
    }
  }
  return NULL;
}

static void hir_ctx_scopes_insert(hir_ctx *ctx, type_entry *entry_ref) {
  hashset_type_entry_ref *scope = list_hir_scope_front(ctx->scopes);
  hashset_type_entry_ref_insert(scope, entry_ref);
  debug("scope_size: %zu", scope->hashset.size);
}

static void hir_ctx_scopes_push(hir_ctx *ctx) {
  list_hir_scope_push_front(ctx->scopes, hashset_type_entry_ref_new());
  debug("scope_height: %zu", list_hir_scope_size(ctx->scopes));
}

static void hir_ctx_scopes_pop(hir_ctx *ctx) {
  hashset_type_entry_ref *ref = list_hir_scope_pop_front(ctx->scopes);
  ctx->scopes->list.f_delete(ref);
  debug("scope_height: %zu", list_hir_scope_size(ctx->scopes));
}

static void hir_ctx_error(hir_ctx *ctx, exception_subtype_hir subtype,
                          const span *span, const char *format, ...) {

  va_list args;
  va_start(args, format);
  hir_exception_add_error_v(ctx->exceptions, subtype, span, format, args);
  va_end(args);
}

static inline void hir_ctx_insert(hir_ctx *ctx, char *id, void *type_base) {
  hir_ctx_scopes_insert(
      ctx, type_table_emplace(ctx->type_table, id, type_base, NULL));
}

// TODO:
// ACTUAL VISITOR FUNCTIONS
// first pass. bind class name and typenames, adding them to global scope
static void hir_bind_builtin(hir_ctx *ctx) {
  hir_ctx_insert(ctx, strdup("bool"), type_primitive_new(TYPE_PRIMITIVE_BOOL));
  hir_ctx_insert(ctx, strdup("byte"), type_primitive_new(TYPE_PRIMITIVE_BYTE));
  hir_ctx_insert(ctx, strdup("int"), type_primitive_new(TYPE_PRIMITIVE_INT));
  hir_ctx_insert(ctx, strdup("uint"), type_primitive_new(TYPE_PRIMITIVE_UINT));
  hir_ctx_insert(ctx, strdup("long"), type_primitive_new(TYPE_PRIMITIVE_LONG));
  hir_ctx_insert(ctx, strdup("ulong"),
                 type_primitive_new(TYPE_PRIMITIVE_ULONG));
  hir_ctx_insert(ctx, strdup("char"), type_primitive_new(TYPE_PRIMITIVE_CHAR));
  hir_ctx_insert(ctx, strdup("string"),
                 type_primitive_new(TYPE_PRIMITIVE_STRING));
  hir_ctx_insert(ctx, strdup("void"), type_primitive_new(TYPE_PRIMITIVE_VOID));
}

static void hir_bind_class_1(hir_ctx *ctx, hir_class *class) {
  type_entry *prev_def;
  if ((prev_def = hir_ctx_scopes_find(ctx, class->id_hir->name))) {
    hir_ctx_error(ctx, EXCEPTION_HIR_TYPE_REDEFINITION, class->base.span, "");
    return;
  }

  // hir_ctx_insert(ctx, class->id->name, type_class_new(strdup(class->id),
  // list_type_ref_new(), list_type_ref_new()));
}

// TODO:
// second pass. bind rest
static void hir_bind_class_2(hir_ctx *ctx, hir_class *class) {}

// TODO:
static void hir_bind_subroutine(hir_ctx *ctx, hir_subroutine *subroutine) {}

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
  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it)) {
    hir_bind_class_1(&ctx, GET(it));
  }
  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it)) {
    hir_bind_class_2(&ctx, GET(it));
  }

  // bind subroutines to global scope
  for (list_hir_subroutine_it it = list_hir_subroutine_begin(hir->subroutines);
       !END(it); NEXT(it)) {
    hir_bind_subroutine(&ctx, GET(it));
  }

  hir_ctx_scopes_pop(&ctx);
  hir_ctx_deinit(&ctx);

  return result;
}
