#include "str.h"
#include "util/log.h"
#include "util/string_buffer.h"

static void string_buffer_append_type(string_buffer       *buffer,
                                      const hir_type_base *generic);

static void string_buffer_append_type_list(string_buffer       *buffer,
                                           const list_hir_type *list,
                                           const char          *prefix,
                                           const char          *suffix) {
  if (list && list_hir_type_size(list)) {
    string_buffer_append(buffer, prefix);
    list_hir_type_it it = list_hir_type_begin(list);
    if (!it.end(&it)) {
      string_buffer_append_type(buffer, it.get(&it));
    }
    while (it.next(&it)) {
      string_buffer_append(buffer, ", ");
      string_buffer_append_type(buffer, it.get(&it));
    }
    string_buffer_append(buffer, suffix);
  }
}

static void string_buffer_append_type(string_buffer       *buffer,
                                      const hir_type_base *generic) {
  if (!generic) {
    string_buffer_append(buffer, "-");
    return;
  }

  switch (generic->type) {
  case HIR_TYPE_BOOL:
    string_buffer_append(buffer, "bool");
    break;
  case HIR_TYPE_BYTE:
    string_buffer_append(buffer, "byte");
    break;
  case HIR_TYPE_INT:
    string_buffer_append(buffer, "int");
    break;
  case HIR_TYPE_UINT:
    string_buffer_append(buffer, "uint");
    break;
  case HIR_TYPE_LONG:
    string_buffer_append(buffer, "long");
    break;
  case HIR_TYPE_ULONG:
    string_buffer_append(buffer, "ulong");
    break;
  case HIR_TYPE_CHAR:
    string_buffer_append(buffer, "char");
    break;
  case HIR_TYPE_STRING:
    string_buffer_append(buffer, "string");
    break;
  case HIR_TYPE_VOID:
    string_buffer_append(buffer, "void");
    break;
  case HIR_TYPE_CUSTOM: {
    hir_type_custom *custom = (hir_type_custom *)generic;
    string_buffer_append(buffer, custom->name);
    string_buffer_append_type_list(buffer, custom->templates, "<", ">");
  } break;
  case HIR_TYPE_ARRAY: {
    hir_type_array *array = (hir_type_array *)generic;
    string_buffer_append(buffer, "array[");
    string_buffer_append_type(buffer, array->elem_type);
    string_buffer_append(buffer, "]");
  } break;
  case HIR_TYPE_CLASS: {
    hir_type_class *class = (hir_type_class *)generic;
    string_buffer_append(buffer, "class ");
    string_buffer_append(buffer, class->name);
    string_buffer_append_type_list(buffer, class->templates, "<", ">");
    string_buffer_append_type_list(buffer, class->parents, " : ", "");
  } break;
  case HIR_TYPE_UNKNOWN:
    warn("unknown type %d %p", generic->type, generic);
    string_buffer_append(buffer, "unknown");
    break;
  }
}

char *hir_type_str(const hir_type_base *generic) {
  if (generic) {
    string_buffer *buffer = string_buffer_new(16, 0);
    string_buffer_append_type(buffer, generic);
    return string_buffer_detach(buffer);
  }
  return NULL;
}
