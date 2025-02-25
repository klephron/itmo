#include "str.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/strbuf.h"
#include <string.h>
// #include "util/hashset.h"

// typedef struct hir_type_map_struct {
//   const type_base *from;
//   const type_base *to;
// } hir_type_map;

// static hir_type_map *hir_type_map_new(const type_base *from,
//                                       const type_base *to) {
//   hir_type_map *self = MALLOC(hir_type_map);
//   self->from = from;
//   self->to = to;
//   return self;
// }

// static void hir_type_map_free(hir_type_map *self) {
//   if (self) {
//     free(self);
//   }
// }

// static inline void container_delete_hir_type_map(void *self) {
//   hir_type_map_free(self);
// }
// static inline int container_cmp_hir_type_map(const void *lsv, const void
// *rsv) {
//   const hir_type_map *l = lsv;
//   const hir_type_map *r = rsv;
//   return container_cmp_ptr(l->from, r->from);
// }
// static inline uint64_t container_hash_hir_type_map(const void *lsv) {
//   const hir_type_map *l = lsv;
//   return container_hash_ptr(l->from);
// }
// HASHSET_DECLARE_STATIC_INLINE(hashset_hir_type_map, hir_type_map,
//                               container_cmp_hir_type_map, container_new_move,
//                               container_delete_hir_type_map,
//                               container_hash_hir_type_map);

// actual str functions
static void strbuf_append_type(strbuf *buffer, const type_base *generic);

static void strbuf_append_type_primitive(strbuf               *buffer,
                                         const type_primitive *type) {
  switch (type->type) {
  case TYPE_PRIMITIVE_BOOL:
    strbuf_append(buffer, "bool");
    break;
  case TYPE_PRIMITIVE_BYTE:
    strbuf_append(buffer, "byte");
    break;
  case TYPE_PRIMITIVE_INT:
    strbuf_append(buffer, "int");
    break;
  case TYPE_PRIMITIVE_UINT:
    strbuf_append(buffer, "uint");
    break;
  case TYPE_PRIMITIVE_LONG:
    strbuf_append(buffer, "long");
    break;
  case TYPE_PRIMITIVE_ULONG:
    strbuf_append(buffer, "ulong");
    break;
  case TYPE_PRIMITIVE_CHAR:
    strbuf_append(buffer, "char");
    break;
  case TYPE_PRIMITIVE_STRING:
    strbuf_append(buffer, "string");
    break;
  case TYPE_PRIMITIVE_VOID:
    strbuf_append(buffer, "void");
    break;
  case TYPE_PRIMITIVE_UNKNOWN:
  default:
    warn("unexpected primitive type %d %p", type->type, type);
  }
}

static void strbuf_append_type_array(strbuf *buffer, const type_array *type) {
  strbuf_append(buffer, "array[");
  strbuf_append_type(buffer, type->element_ref);
  strbuf_append(buffer, "]");
}

static void strbuf_append_type_callable(strbuf              *buffer,
                                        const type_callable *type) {
  strbuf_append(buffer, "(");
  list_type_ref_it it = list_type_ref_begin(type->params);
  if (!END(it)) {
    strbuf_append_type(buffer, GET(it));
    for (NEXT(it); !END(it); NEXT(it)) {
      strbuf_append(buffer, ", ");
      strbuf_append_type(buffer, GET(it));
    }
  }
  strbuf_append(buffer, ") -> ");
  strbuf_append_type(buffer, type->ret_ref);
}

static void strbuf_append_type_class(strbuf *buffer, const type_class *type) {
  strbuf_append(buffer, "class ");
  strbuf_append(buffer, type->id ? type->id : "(nil)");
  {
    list_type_ref_it it = list_type_ref_begin(type->typenames);
    if (!END(it)) {
      strbuf_append(buffer, " <");
      strbuf_append_type(buffer, GET(it));
      for (NEXT(it); !END(it); NEXT(it)) {
        strbuf_append(buffer, ", ");
        strbuf_append_type(buffer, GET(it));
      }
      strbuf_append(buffer, ">");
    }
  }
  {
    list_type_ref_it it = list_type_ref_begin(type->parents);
    if (!END(it)) {
      strbuf_append(buffer, " : ");
      strbuf_append_type(buffer, GET(it));
      for (NEXT(it); !END(it); NEXT(it)) {
        strbuf_append(buffer, ", ");
        strbuf_append_type(buffer, GET(it));
      }
    }
  }
}

static void strbuf_append_type_typename(strbuf              *buffer,
                                        const type_typename *type) {
  strbuf_append(buffer, "typename ");
  strbuf_append(buffer, type->id);
}

static void strbuf_append_type_mono(strbuf *buffer, const type_mono *type) {
  strbuf_append(buffer, "mono(");
  strbuf_append_type(buffer, type->type_ref);
  strbuf_append(buffer, " with ");
  list_type_ref_it itm = list_type_ref_begin(type->types);
  if (!END(itm)) {
    strbuf_append_type(buffer, GET(itm));
    for (NEXT(itm); !END(itm); NEXT(itm)) {
      strbuf_append(buffer, ", ");
      strbuf_append_type(buffer, GET(itm));
    }
  }
  strbuf_append(buffer, ")");
}

static void strbuf_append_type(strbuf *buffer, const type_base *generic) {
  if (!generic) {
    strbuf_append(buffer, "any");
    return;
  }

  switch (generic->kind) {
  case TYPE_PRIMITIVE:
    strbuf_append_type_primitive(buffer, (const type_primitive *)generic);
    break;
  case TYPE_ARRAY:
    strbuf_append_type_array(buffer, (const type_array *)generic);
    break;
  case TYPE_CALLABLE:
    strbuf_append_type_callable(buffer, (const type_callable *)generic);
    break;
  case TYPE_CLASS:
    strbuf_append_type_class(buffer, (const type_class *)generic);
    break;
  case TYPE_TYPENAME:
    strbuf_append_type_typename(buffer, (const type_typename *)generic);
    break;
  case TYPE_MONO:
    strbuf_append_type_mono(buffer, (const type_mono *)generic);
    break;
  case TYPE_UNKNOWN:
  default:
    warn("unexpected type kind %d %p", generic->kind, generic);
    break;
  }
}

const char *type_enum_str(type_enum kind) {
  switch (kind) {
  case TYPE_PRIMITIVE:
    return "PRIMITIVE";
  case TYPE_ARRAY:
    return "ARRAY";
  case TYPE_CALLABLE:
    return "CALLABLE";
  case TYPE_CLASS:
    return "CLASS";
  case TYPE_TYPENAME:
    return "TYPENAME";
  case TYPE_MONO:
    return "MONO";
  case TYPE_UNKNOWN:
  default:
    warn("unknown type kind %d", kind);
    return "UNKNOWN";
  }
}

char *type_str(const type_base *generic) {
  if (generic) {
    strbuf *buffer = strbuf_new(16, 0);
    strbuf_append_type(buffer, generic);
    return strbuf_detach(buffer);
  }
  return NULL;
}

const char *type_mono_enum_str(type_mono_enum spec) {
  switch (spec) {
  case TYPE_MONO_UNSET:
    return "UNSET";
  case TYPE_MONO_FULL:
    return "FULL";
  case TYPE_MONO_NOT_FULL:
    return "NOT_FULL";
  default:
    warn("unknown type_spec %d", spec);
    return "UNKNOWN";
  }
}
