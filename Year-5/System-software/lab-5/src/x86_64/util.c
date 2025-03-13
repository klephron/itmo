#include "util.h"

const char *__x86_64_type_enum_str(x86_64_type_enum type) {
  switch (type) {
    case X86_64_TYPE_VOID:
      return "void";
    case X86_64_TYPE_BOOL:
      return "bool";
    case X86_64_TYPE_BYTE:
      return "byte";
    case X86_64_TYPE_INT:
      return "int";
    case X86_64_TYPE_UINT:
      return "uint";
    case X86_64_TYPE_LONG:
      return "long";
    case X86_64_TYPE_ULONG:
      return "ulong";
    case X86_64_TYPE_CHAR:
      return "char";
    case X86_64_TYPE_STRING:
      return "string";
    case X86_64_TYPE_ARRAY:
      return "array";
    case X86_64_TYPE_OBJECT:
      return "object";
    case X86_64_TYPE_REF_VALUE:
      return "ref_value";
    case X86_64_TYPE_ERROR:
      return "error";
    case X86_64_TYPE__COUNT:
    default:
      return "<unknown>";
  }
}
