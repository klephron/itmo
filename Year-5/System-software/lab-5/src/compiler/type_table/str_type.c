#include "str.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/strbuf.h"
#include <string.h>

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

static void strbuf_append_type_specialized(strbuf          *buffer,
                                           const type_mono *type) {
  strbuf_append(buffer, "mono ");
  strbuf_append_type(buffer, type->type_ref);
  strbuf_append(buffer, " with (");
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
    strbuf_append_type_specialized(buffer, (const type_mono *)generic);
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
    return "MONOMORPHIZED";
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
