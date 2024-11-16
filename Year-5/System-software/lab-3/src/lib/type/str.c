#include "str.h"

#include "util/string_buffer.h"
#include <stdio.h>
#include <string.h>

static void type_str_generic(string_buffer *buffer, const type_base *generic) {
  if (!generic) {
    string_buffer_append(buffer, "(nil)");
    return;
  }

  switch (generic->type) {
  case TYPE_BOOL:
    string_buffer_append(buffer, "bool");
    break;
  case TYPE_BYTE:
    string_buffer_append(buffer, "byte");
    break;
  case TYPE_INT:
    string_buffer_append(buffer, "int");
    break;
  case TYPE_UINT:
    string_buffer_append(buffer, "uint");
    break;
  case TYPE_LONG:
    string_buffer_append(buffer, "long");
    break;
  case TYPE_ULONG:
    string_buffer_append(buffer, "ulong");
    break;
  case TYPE_CHAR:
    string_buffer_append(buffer, "char");
    break;
  case TYPE_STRING:
    string_buffer_append(buffer, "string");
    break;
  case TYPE_VOID:
    string_buffer_append(buffer, "void");
    break;
  case TYPE_ARRAY: {
    type_array *array = (type_array *)generic;
    string_buffer_append(buffer, "array[");
    type_str_generic(buffer, array->elem_type);
    string_buffer_append(buffer, "]");
  } break;
  case TYPE_CUSTOM: {
    type_custom *custom = (type_custom *)generic;
    string_buffer_append(buffer, custom->name);
  } break;
  case TYPE_CALLABLE: {
    type_callable *callable = (type_callable *)generic;
    string_buffer_append(buffer, "(");

    size_t       argc = list_type_size(callable->param_types);
    list_type_it it = list_type_begin(callable->param_types);
    if (argc) {
      type_base *type = list_type_it_get(&it);
      type_str_generic(buffer, type);
      list_type_it_next(&it);
    }
    for (size_t i = 1; i < argc; ++i, list_type_it_next(&it)) {
      string_buffer_append(buffer, ",");
      type_base *type = list_type_it_get(&it);
      type_str_generic(buffer, type);
    }
    string_buffer_append(buffer, ")->");
    type_str_generic(buffer, callable->ret_type);
  } break;
  case TYPE_UNKNOWN:
  default:
    string_buffer_append(buffer, "unknown");
    break;
  }
}

char *type_str(const type_base *generic) {
  if (generic) {
    string_buffer *buffer = string_buffer_new(16, 0);
    type_str_generic(buffer, generic);
    return string_buffer_detach(buffer);
  }
  return NULL;
}
