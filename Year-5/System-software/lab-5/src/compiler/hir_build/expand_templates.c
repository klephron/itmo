#include "expand_templates.h"

#include "compiler/type_table/str.h"
#include "util/hashset.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdio.h>

// 1. erase classes with templates from hir list, add to the list for expansion
// 2. add templates to list of to be expanded
// 3. expand preorder, add types that aren't currently expanded
// 4. infinite recursion -> stack overflow (maybe handle different way later)

HASHSET_DECLARE_STATIC_INLINE(hashset_hir_class_ref, hir_class,
                              container_cmp_ptr, container_new_move,
                              container_delete_false, container_hash_ptr);

HASHSET_DECLARE_STATIC_INLINE(hashset_type_mono_ref, type_mono,
                              container_cmp_ptr, container_new_move,
                              container_delete_false, container_hash_ptr);

// CLASS_ENTRY
typedef struct hir_type_tmpl_struct {
  const type_mono *type_ref; // which mono to instantiate (should be mono-full)
  const hir_base  *template_ref; // which hir class template to use
} hir_type_tmpl;

static void hir_type_tmpl_free(hir_type_tmpl *self) {
  if (self) {
    free(self);
  }
}

static inline void container_delete_hir_type_tmpl(void *self) {
  hir_type_tmpl_free(self);
}
LIST_DECLARE_STATIC_INLINE(list_hir_type_tmpl, hir_type_tmpl,
                           container_cmp_false, container_new_move,
                           container_delete_hir_type_tmpl);

static inline int container_cmp_hir_type_tmpl(const void *lsv,
                                              const void *rsv) {
  const hir_type_tmpl *l = lsv;
  const hir_type_tmpl *r = rsv;
  return container_cmp_ptr(l->type_ref, r->type_ref);
}
static inline uint64_t container_hash_hir_type_tmpl(const void *lsv) {
  const hir_type_tmpl *l = lsv;
  return container_hash_ptr(l->type_ref);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_hir_type_tmpl, hir_type_tmpl,
                              container_cmp_hir_type_tmpl, container_new_move,
                              container_delete_hir_type_tmpl,
                              container_hash_hir_type_tmpl);

// CONTEXT
typedef struct hir_ctx_struct {
  list_hir_type_tmpl    *class_queue; // for expansion (like in BFS)
  hashset_type_mono_ref *inst_types;
  hashset_hir_type_tmpl *type_templates; // references type template for inst
  list_exception        *exceptions;
} hir_ctx;

static void hir_ctx_init(hir_ctx *ctx, list_exception *exceptions) {
  ctx->class_queue = list_hir_type_tmpl_new();
  ctx->inst_types = hashset_type_mono_ref_new();
  ctx->type_templates = hashset_hir_type_tmpl_new();
  ctx->exceptions = exceptions;
}

static void hir_ctx_deinit(hir_ctx *ctx) {
  list_hir_type_tmpl_free(ctx->class_queue);
  hashset_type_mono_ref_free(ctx->inst_types);
  hashset_hir_type_tmpl_free(ctx->type_templates);
  ctx->exceptions = NULL;
}

// returns classes that need to be instantiated (used for template expansion)
static list_hir_class *hir_filter_mono_full_hir_classes(hir_ctx *ctx,
                                                        hir     *hir) {
  UNUSED(ctx);
  list_hir_class *classes = list_hir_class_new();
  list_hir_class *classes_t = list_hir_class_new(); // to be expanded

  for (list_hir_class_it it = list_hir_class_begin(hir->classes); !END(it);
       NEXT(it)) {
    hir_class *hir_class = GET(it);

    if (!(hir_class->state &= HIR_STATE_BIND_TYPE)) {
      error("illegal state, expected class type %s type to be binded");
      continue;
    }

    type_base *class_type = hir_class->type_ref->type;

    type_mono_enum type_mono = type_fetch_mono(class_type);
    switch (type_mono) {
    case TYPE_MONO_NOT_FULL: {
      list_hir_class_push_back(classes_t, hir_class);
      break;
    }
    case TYPE_MONO_FULL: {
      list_hir_class_push_back(classes, hir_class);
      break;
    }
    case TYPE_MONO_UNSET:
    default: {
      char *type_s = type_str(class_type);
      error("mono should be set for class with type %s %p", type_s, hir_class);
      if (type_s) {
        free(type_s);
      }
      break;
    }
    }
  }

  // cuz iterator invalidates when elements are deleted
  while (!list_hir_class_empty(hir->classes)) {
    list_hir_class_pop_front(hir->classes);
  }
  list_hir_class_free(hir->classes);
  hir->classes = classes;

  return classes_t;
}

// get class
static list_type_ref *hir_get_mono_full_classes(hir_ctx    *ctx,
                                                type_table *type_table) {
  UNUSED(ctx);
  list_type_ref *classes_t = list_type_ref_new();

  for (list_type_entry_it it = list_type_entry_begin(type_table->entries);
       !END(it); NEXT(it)) {
    const type_entry *entry = GET(it);
    type_base        *type = entry->type;

    type_mono_enum mono = type_fetch_mono(type);
    switch (mono) {
    case TYPE_MONO_FULL: {
      if (type->kind == TYPE_MONO) {
        type_mono *mono = (type_mono *)type;
        type_enum  kind = mono->type_ref->kind;
        if (kind == TYPE_CLASS) {
          list_type_ref_push_back(classes_t, type);
        } else if (mono->type_ref->kind == TYPE_MONO) {
          error("unexpected mono as base for mono (%p -> %p)", mono,
                mono->type_ref);
        }
      }
      break;
    }
    case TYPE_MONO_NOT_FULL: {
      break;
    }
    case TYPE_MONO_UNSET:
    default: {
      char *type_s = type_str(type);
      error("mono should be set for class with type %s %p", type_s, type);
      if (type_s) {
        free(type_s);
      }
      break;
    }
    }
  }
  return classes_t;
}

hir_expand_templates_result hir_expand_templates(hir        *hir,
                                                 type_table *type_table) {
  hir_expand_templates_result result = {
      .exceptions = list_exception_new(),
  };

  hir_ctx ctx;
  hir_ctx_init(&ctx, result.exceptions);

  list_hir_class *classes_t = hir_filter_mono_full_hir_classes(&ctx, hir);
  list_type_ref  *class_types_t = hir_get_mono_full_classes(&ctx, type_table);

  debug("initial classes to be instantiated:");
  for (list_type_ref_it it = list_type_ref_begin(class_types_t); !END(it);
       NEXT(it)) {
    char *type_s = type_str(GET(it));
    debug("  %s", type_s);
    if (type_s) {
      free(type_s);
    }
  }
  list_type_ref_free(class_types_t);

  list_hir_class_free(classes_t);
  hir_ctx_deinit(&ctx);

  return result;
}
