#include "expand_templates.h"

#include "compiler/hir_build/exception.h"
#include "compiler/type_table/str.h"
#include "util/hashset.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdio.h>
#include <string.h>

// 1. erase classes with templates from hir list, add to the list for expansion
// 2. add templates to list of to be expanded
// 3. expand preorder, add types that aren't currently expanded
// 4. infinite recursion -> stack overflow (maybe handle different way later)

LIST_DECLARE_STATIC_INLINE(list_type_mono_ref, type_mono, container_cmp_false,
                           container_new_move, container_delete_false);

// TYPE_TEMPLATE ENTRY (for resolution)
typedef struct hir_type_tmpl_struct {
  const type_base *type_ref; // which type template to use (is not mono-full)
  const hir_base  *template_ref; // which hir class template to use
} hir_type_tmpl;

static hir_type_tmpl *hir_type_tmpl_new(const type_base *type_ref,
                                        const hir_base  *template_ref) {
  hir_type_tmpl *self = MALLOC(hir_type_tmpl);
  self->type_ref = type_ref;
  self->template_ref = template_ref;
  return self;
}

static void hir_type_tmpl_free(hir_type_tmpl *self) {
  if (self) {
    free(self);
  }
}

static inline void container_delete_hir_type_tmpl(void *self) {
  hir_type_tmpl_free(self);
}
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

// RESOLVED TYPES (including builtin)
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

// TYPE_MAPPINGS
typedef struct hir_type_map_struct {
  const type_base *from;
  const type_base *to;
} hir_type_map;

static hir_type_map *hir_type_map_new(const type_base *from,
                                      const type_base *to) {
  hir_type_map *self = MALLOC(hir_type_map);
  self->from = from;
  self->to = to;
  return self;
}

static void hir_type_map_free(hir_type_map *self) {
  if (self) {
    free(self);
  }
}

static inline void container_delete_hir_type_map(void *self) {
  hir_type_map_free(self);
}
static inline int container_cmp_hir_type_map(const void *lsv, const void *rsv) {
  const hir_type_map *l = lsv;
  const hir_type_map *r = rsv;
  return container_cmp_ptr(l->from, r->from);
}
static inline uint64_t container_hash_hir_type_map(const void *lsv) {
  const hir_type_map *l = lsv;
  return container_hash_ptr(l->from);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_hir_type_map, hir_type_map,
                              container_cmp_hir_type_map, container_new_move,
                              container_delete_hir_type_map,
                              container_hash_hir_type_map);

// CONTEXT
typedef struct hir_ctx_struct {
  hashset_hir_type_tmpl *map_type_hir_tmpl; // matches types and their templates
  list_hir_class        *hir_tmpl_classes;  // stores actual class templates
  hashset_type_ref      *insted_types; // already instantiated/resolved types
  hashset_type_ref      *registered_types; // present in type table

  // no scopes as there is not constructions for recursive expansion
  hashset_hir_type_map *map_type_tmpl;
  list_type_mono_ref   *instto_types;

  type_table     *type_table;
  list_hir_class *hir_classes;
  list_exception *exceptions;
} hir_ctx;

static void hir_ctx_init(hir_ctx *ctx, type_table *type_table,
                         list_exception *exceptions,
                         list_hir_class *hir_classes) {
  ctx->map_type_hir_tmpl = hashset_hir_type_tmpl_new();
  ctx->hir_tmpl_classes = list_hir_class_new();
  ctx->insted_types = hashset_type_ref_new();
  ctx->registered_types = hashset_type_ref_new();

  ctx->map_type_tmpl = NULL;
  ctx->instto_types = NULL;

  ctx->type_table = type_table;
  ctx->hir_classes = hir_classes;
  ctx->exceptions = exceptions;
}

static void hir_ctx_deinit(hir_ctx *ctx) {
  hashset_hir_type_tmpl_free(ctx->map_type_hir_tmpl);
  list_hir_class_free(ctx->hir_tmpl_classes);
  hashset_type_ref_free(ctx->insted_types);
  hashset_type_ref_free(ctx->registered_types);

  ctx->map_type_tmpl = NULL;

  ctx->type_table = NULL;
  ctx->hir_classes = NULL;
  ctx->exceptions = NULL;
}

static const type_base *hir_ctx_type_tmpl_find(hir_ctx         *ctx,
                                               const type_base *type) {
  hashset_hir_type_map_it it = hashset_hir_type_map_find(
      ctx->map_type_tmpl, &(hir_type_map){.from = type});
  if (END(it)) {
    return NULL;
  }
  return GET(it)->to;
}

// add to registered types and add to type_table
static type_base *hir_ctx_type_register(hir_ctx *ctx, type_base *type,
                                        const span *span_ref) {
  hashset_type_ref_it it = hashset_type_ref_find(ctx->registered_types, type);
  if (END(it)) {
    hashset_type_ref_insert(ctx->registered_types, type);
    type_entry *entry =
        type_table_emplace(ctx->type_table, type, span_copy(span_ref));
    return entry->type;
  }
  type_free(type);
  return GET(it);
}

static void hir_ctx_debug(hir_ctx *ctx) {
  debug("hir_ctx:");
  debug("  map_type_templates -> size: %lu",
        ctx->map_type_hir_tmpl->hashset.size);
  debug("  insted_types -> size: %lu", ctx->insted_types->hashset.size);
  debug("  class_tmpls -> size: %lu",
        list_hir_class_size(ctx->hir_tmpl_classes));
  debug("  exceptions -> size: %lu", list_exception_size(ctx->exceptions));
}

// filters out templated classes from hir->classes
// rest stay in hir->class, templates returned by this function
static list_hir_class *hir_filter_templated_hir_classes(hir *hir) {
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

// get class types that need to be instantiated
static list_type_mono_ref *
hir_get_mono_full_class_types(type_table *type_table) {
  list_type_mono_ref *classes_t = list_type_mono_ref_new();

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
          list_type_mono_ref_push_back(classes_t, mono);
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

// adds errors if can't instantiate
static const hir_base *hir_get_hir_template(hir_ctx         *ctx,
                                            const type_mono *mono) {
  type_enum kind = mono->base.kind;

  if (kind != TYPE_MONO) {
    error("expecting mono types to fill class_inst_q, got %s(%d)",
          type_enum_str(kind), kind);
    return NULL;
  }

  hashset_hir_type_tmpl_it it = hashset_hir_type_tmpl_find(
      ctx->map_type_hir_tmpl, &(hir_type_tmpl){.type_ref = mono->type_ref});

  if (!END(it)) {
    return GET(it)->template_ref;
  }

  char *type_s = type_str((type_base *)mono);
  hir_exception_add_error(ctx->exceptions, EXCEPTION_HIR_TEMPLATE_EXPANSION,
                          mono->base.type_entry_ref->span,
                          "can't find template to instantiate %s", type_s);
  if (type_s) {
    free(type_s);
  }

  return NULL;
}

// add class templates that can be used for instantiation
static void hir_setup_ctx_hir_classes(hir_ctx        *ctx,
                                      list_hir_class *templated_classes) {
  while (!list_hir_class_empty(templated_classes)) {
    hir_class     *hir = list_hir_class_pop_front(templated_classes);
    hir_type_tmpl *type_tmpl =
        hir_type_tmpl_new(hir->type_ref->type, (hir_base *)hir);

    hashset_hir_type_tmpl_insert(ctx->map_type_hir_tmpl, type_tmpl);

    list_hir_class_push_back(ctx->hir_tmpl_classes, hir);
  }
  list_hir_class_free(templated_classes);
}

static void hir_setup_ctx_types(hir_ctx *ctx, type_table *type_table) {
  for (list_type_entry_it it = list_type_entry_begin(type_table->entries);
       !END(it); NEXT(it)) {
    hashset_type_ref_insert(ctx->registered_types, GET(it)->type);
  }
}

static hir_var *hir_expand_var(hir_ctx *ctx, const hir_var *var_t) {
  return NULL;
}

static hir_subroutine *
hir_expand_subroutine(hir_ctx *ctx, const hir_subroutine *subroutine_t) {
  return NULL;
}

// FIX: unwrap recursively, because constructions like
// class B<T> : A<array [] of T> doesn't work
// how: expand type_mono passing map(template -> actual) then compare.
// this function is also needed when handling vars and methods
// hir_expand_type()
static type_base *hir_expand_type_definition(hir_ctx *ctx, type_mono *type_mono,
                                             int *changed) {}

// if any updates then instantiate new template with resolved types
static void hir_expand_class_parents(hir_ctx          *ctx,
                                     const type_class *class_t_type,
                                     type_mono        *class_type) {
  int            parents_modified = 0;
  list_type_ref *parents = list_type_ref_new();

  for (list_type_ref_it it = list_type_ref_begin(class_t_type->parents);
       !END(it); NEXT(it)) {
    union {
      type_base *base;
      type_mono *mono;
    } old_parent;

    old_parent.base = GET(it);

    // if not full monoed, need to create new mono type and resolve typenames
    if (type_fetch_mono(old_parent.base) == TYPE_MONO_FULL) {
      list_type_ref_push_back(parents, old_parent.base);
      continue;
    }

    if (old_parent.base->kind != TYPE_MONO) {
      error("expected monoed parent (or class), got %d", old_parent.base->kind);
      continue;
    }

    type_mono *new_parent =
        type_mono_new(old_parent.mono->type_ref, list_type_ref_new());

    for (list_type_ref_it p_it = list_type_ref_begin(old_parent.mono->types);
         !END(p_it); NEXT(p_it)) {

      // FIX: --------------- this function will be inside hir_expand_type
      // because it should work recursively,
      // maybe even resolve_parent function idk

      const type_base *p_from = GET(p_it);
      const type_base *p_to;

      if (!(p_to = hir_ctx_type_tmpl_find(ctx, p_from))) {
        list_type_ref_push_back(new_parent->types, (type_base *)p_from);
      } else {
        list_type_ref_push_back(new_parent->types, (type_base *)p_to);
      }

      // FIX: --------------- whole this function will be inside hir_expand_type
    }

    // register to type table + add to inst list + add to modified parents
    new_parent = (type_mono *)hir_ctx_type_register(
        ctx, (type_base *)new_parent, old_parent.base->type_entry_ref->span);
    parents_modified = 1;

    list_type_mono_ref_push_back(ctx->instto_types, new_parent);
    list_type_ref_push_back(parents, (type_base *)new_parent);
  }

  // need to create new templated type, because parents are modified
  if (parents_modified) {
    type_class *new_class =
        type_class_new(strdup(class_t_type->id),
                       type_copy_list_ref(class_t_type->typenames), parents);

    new_class = (type_class *)hir_ctx_type_register(
        ctx, (type_base *)new_class, class_t_type->base.type_entry_ref->span);
    class_type->type_ref = (type_base *)new_class;
  } else {
    list_type_ref_free(parents);
  }
}

static void hir_expand_class(hir_ctx *ctx, type_mono *class_type) {

  hashset_type_ref_it found =
      hashset_type_ref_find(ctx->insted_types, (type_base *)class_type);
  if (!END(found)) {
    return;
  }

  const hir_class *class_t = (hir_class *)hir_get_hir_template(ctx, class_type);
  if (!class_t) {
    return;
  }

  // setup templates that need to be replaced
  ctx->map_type_tmpl = hashset_hir_type_map_new();

  const type_class *class_t_type = (type_class *)class_t->type_ref->type;

  for (list_type_ref_it fit = list_type_ref_begin(class_t_type->typenames),
                        tit = list_type_ref_begin(class_type->types);
       !END(tit); NEXT(fit), NEXT(tit)) {
    type_base *from_type = GET(fit);
    type_base *to_type = GET(tit);
    hashset_hir_type_map_insert(ctx->map_type_tmpl,
                                hir_type_map_new(from_type, to_type));
  }

  // accumulate types for further expansion, expands in non-recursive manner
  ctx->instto_types = list_type_mono_ref_new();

  hir_expand_class_parents(ctx, class_t_type, class_type);

  hir_class *class =
      hir_class_new_typed(class_type->base.type_entry_ref, list_hir_var_new(),
                          list_hir_method_new());

  for (list_hir_var_it it = list_hir_var_begin(class_t->fields); !END(it);
       NEXT(it)) {
    hir_var *var = hir_expand_var(ctx, GET(it));
    if (var) {
      list_hir_var_push_back(class->fields, var);
    }
  }

  for (list_hir_method_it it = list_hir_method_begin(class_t->methods);
       !END(it); NEXT(it)) {
    const hir_method *method_t = GET(it);
    hir_subroutine   *sub = hir_expand_subroutine(ctx, method_t->subroutine);
    if (sub) {
      list_hir_method_push_back(class->methods,
                                hir_method_new(span_copy(method_t->base.span),
                                               method_t->modifier, sub));
    }
  }

  // prepare for next expansions
  hashset_type_ref_insert(ctx->insted_types, (type_base *)class_type);
  list_hir_class_push_back(ctx->hir_classes, class);

  hashset_hir_type_map_free(ctx->map_type_tmpl);
  list_type_mono_ref *instto_types = ctx->instto_types;
  ctx->instto_types = NULL;

  // expand further recursively to fall on stack overflow if infinite loop =)
  for (list_type_mono_ref_it it = list_type_mono_ref_begin(instto_types);
       !END(it); NEXT(it)) {
    type_mono *mono = GET(it);
    switch (mono->type_ref->kind) {
    case TYPE_CLASS:
      hir_expand_class(ctx, mono);
      break;
    default: {
      char *type_s = type_str((type_base *)mono);
      error("expansion of type %s is unsupported", type_s);
      if (type_s) {
        free(type_s);
      }
      break;
    }
    }
  }
  list_type_mono_ref_free(instto_types);
}

hir_expand_templates_result hir_expand_templates(hir        *hir,
                                                 type_table *type_table) {
  hir_expand_templates_result result = {
      .exceptions = list_exception_new(),
  };

  hir_ctx ctx;

  list_hir_class *templated_classes = hir_filter_templated_hir_classes(hir);

  // will append classes and exceptions
  hir_ctx_init(&ctx, type_table, result.exceptions, hir->classes);
  // hir_register_types(&)
  hir_setup_ctx_hir_classes(&ctx, templated_classes);
  hir_setup_ctx_types(&ctx, type_table);

  hir_ctx_debug(&ctx);

  // expand classes
  list_type_mono_ref *mono_class_types =
      hir_get_mono_full_class_types(type_table);

  for (list_type_mono_ref_it it = list_type_mono_ref_begin(mono_class_types);
       !END(it); NEXT(it)) {
    type_mono *mono = GET(it);

    switch (mono->type_ref->kind) {
    case TYPE_CLASS:
      hir_expand_class(&ctx, GET(it));
      break;
    default: {
      char *type_s = type_str((type_base *)mono);
      error("expansion of type %s is unsupported", type_s);
      if (type_s) {
        free(type_s);
      }
      break;
    }
    }
  }
  list_type_mono_ref_free(mono_class_types);

  hir_ctx_debug(&ctx);

  hir_ctx_deinit(&ctx);

  return result;
}
