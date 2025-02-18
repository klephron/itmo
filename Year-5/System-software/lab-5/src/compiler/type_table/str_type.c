#include "str.h"
#include "util/log.h"
#include "util/macro.h"
#include <string.h>

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
  case TYPE_SPECIALIZED:
    return "SPECIALIZED";
  case TYPE_UNKNOWN:
  default:
    warn("unknown type kind %d", kind);
    return "UNKNOWN";
  }
}

char *type_str(type_base *generic) {
  UNUSED(generic);
  return strdup("(type)");
}
