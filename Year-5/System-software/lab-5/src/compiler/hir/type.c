#include "type.h"

#include "util/macro.h"

#include <assert.h>
#include <stdlib.h>
#include <string.h>

static void hir_type_base_deinit(hir_type_base *self) { span_free(self->span); }

static void hir_type_base_free(hir_type_base *self) {
  if (self) {
    hir_type_base_deinit(self);
    free(self);
  }
}

static void hir_type_base_init(hir_type_base *self, hir_type_enum type,
                               span *span) {
  self->type = type;
  self->span = span;
}

static void hir_type_custom_free(hir_type_custom *self) {
  if (self) {
    if (self->name) {
      free(self->name);
    }
    list_hir_type_free(self->templates);
    hir_type_base_deinit(&self->base);
    free(self);
  }
}

static void hir_type_array_free(hir_type_array *self) {
  if (self) {
    hir_type_free(self->elem_type);
    hir_type_base_deinit(&self->base);
    free(self);
  }
}

static void hir_type_class_free(hir_type_class *self) {
  if (self) {
    list_hir_type_free(self->templates);
    list_hir_type_free(self->parents);
    hir_type_base_deinit(&self->base);
    free(self);
  }
}

const char *hir_type_enum_str(hir_type_enum type) {
  switch (type) {
  case HIR_TYPE_BOOL:
    return "BOOL";
  case HIR_TYPE_BYTE:
    return "BYTE";
  case HIR_TYPE_INT:
    return "INT";
  case HIR_TYPE_UINT:
    return "UINT";
  case HIR_TYPE_LONG:
    return "LONG";
  case HIR_TYPE_ULONG:
    return "ULONG";
  case HIR_TYPE_CHAR:
    return "CHAR";
  case HIR_TYPE_STRING:
    return "STRING";
  case HIR_TYPE_VOID:
    return "VOID";
  case HIR_TYPE_CUSTOM:
    return "CUSTOM";
  case HIR_TYPE_ARRAY:
    return "ARRAY";
  case HIR_TYPE_CLASS:
    return "CLASS";
  case HIR_TYPE_UNKNOWN:
  default:
    return "UNKNOWN";
  }
}

hir_type_base *hir_type_base_new(span *span, hir_type_enum type) {
  hir_type_base *self = MALLOC(hir_type_base);
  hir_type_base_init(self, type, span);
  return self;
}

const char *hir_type_base_str(hir_type_base *self) {
  if (self) {
    return hir_type_enum_str(self->type);
  }
  return "<null>";
}

void hir_type_free(hir_type_base *generic) {
  if (generic) {
    switch (generic->type) {
    case HIR_TYPE_CUSTOM:
      return hir_type_custom_free((hir_type_custom *)generic);
    case HIR_TYPE_ARRAY:
      return hir_type_array_free((hir_type_array *)generic);
    case HIR_TYPE_CLASS:
      return hir_type_class_free((hir_type_class *)generic);
    default:
      return hir_type_base_free(generic);
    }
  }
}

// if both types are not null and different -> not equal
int hir_type_equals(const hir_type_base *lsv, const hir_type_base *rsv) {
  if (!lsv || !rsv) {
    return 1;
  }
  if (lsv->type != rsv->type) {
    return 0;
  }
  switch (lsv->type) {
  case HIR_TYPE_CUSTOM: {
    hir_type_custom *lsv_unr = (hir_type_custom *)lsv;
    hir_type_custom *rsv_unr = (hir_type_custom *)rsv;
    return !strcmp(lsv_unr->name, rsv_unr->name);
  }
  case HIR_TYPE_ARRAY: {
    hir_type_array *lsv_arr = (hir_type_array *)lsv;
    hir_type_array *rsv_arr = (hir_type_array *)rsv;
    return hir_type_equals(lsv_arr->elem_type, rsv_arr->elem_type);
  }
  case HIR_TYPE_CLASS: {
    hir_type_class *lsv_cl = (hir_type_class *)lsv;
    hir_type_class *rsv_cl = (hir_type_class *)rsv;
    if (!list_hir_type_equals(lsv_cl->parents, rsv_cl->parents)) {
      return 0;
    }
    if (!list_hir_type_equals(lsv_cl->templates, rsv_cl->templates)) {
      return 0;
    }
    return 1;
  }
  default:
    return 1;
  }
}

hir_type_base *hir_type_copy(const hir_type_base *self) {
  if (!self) {
    return NULL;
  }
  switch (self->type) {
  case HIR_TYPE_CUSTOM: {
    hir_type_custom *custom = (hir_type_custom *)self;
    return (hir_type_base *)hir_type_custom_new(
        span_copy(self->span), strdup(custom->name),
        list_hir_type_copy(custom->templates));
  }
  case HIR_TYPE_ARRAY: {
    hir_type_array *array = (hir_type_array *)self;
    return (hir_type_base *)hir_type_array_new(span_copy(self->span),
                                               hir_type_copy(array->elem_type));
  }
  case HIR_TYPE_CLASS: {
    hir_type_class *class = (hir_type_class *)self;
    return (hir_type_base *)hir_type_class_new(
        span_copy(self->span), strdup(class->name),
        list_hir_type_copy(class->templates),
        list_hir_type_copy(class->parents));
  }
  default:
    return hir_type_base_new(span_copy(self->span), self->type);
  }
}

int list_hir_type_equals(const list_hir_type *lsv, const list_hir_type *rsv) {
  size_t lsize = list_hir_type_size(lsv);
  size_t rsize = list_hir_type_size(rsv);
  if (lsize != rsize) {
    return 0;
  }
  for (list_node *lit = lsv->list.head, *rit = rsv->list.head; lit;
       lit = lit->next, rit = rit->next) {
    int ret = hir_type_equals(lit->data, rit->data);
    if (!ret) {
      return 0;
    }
  }
  return 1;
}

list_hir_type *list_hir_type_copy(const list_hir_type *in) {
  if (!in) {
    return NULL;
  }
  list_hir_type *nlist = list_hir_type_new();
  for (list_hir_type_it it = list_hir_type_begin((list_hir_type *)in);
       !it.end(&it); it.next(&it)) {
    list_hir_type_push_back(nlist, hir_type_copy(it.get(&it)));
  }
  return nlist;
}

hir_type_custom *hir_type_custom_new(span *span, char *name,
                                     list_hir_type *templates) {
  hir_type_custom *self = MALLOC(hir_type_custom);
  hir_type_base_init(&self->base, HIR_TYPE_CUSTOM, span);
  self->name = name;
  self->templates = templates;
  return self;
}

hir_type_array *hir_type_array_new(span *span, hir_type_base *elem_type) {
  hir_type_array *self = MALLOC(hir_type_array);
  hir_type_base_init(&self->base, HIR_TYPE_ARRAY, span);
  self->elem_type = elem_type;
  return self;
}

hir_type_class *hir_type_class_new(span *span, char *name,
                                   list_hir_type *templates,
                                   list_hir_type *parents) {
  hir_type_class *self = MALLOC(hir_type_class);
  hir_type_base_init(&self->base, HIR_TYPE_CLASS, span);
  self->name = name;
  self->templates = templates;
  self->parents = parents;
  return self;
}
