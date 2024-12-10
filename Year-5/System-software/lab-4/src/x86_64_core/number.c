#include "number.h"

#include <stdio.h>

x86_64_number x86_64_number_get(x86_64_data *data) {
  x86_64_number number;

  x86_64_type_enum kind = data->type->entries[0].kind;
  switch (kind) {
  case X86_64_TYPE_INT: {
    int32_t r = *(int32_t *)data->raw;
    number.ld = r;
    number.sign = 1;
  } break;
  case X86_64_TYPE_LONG: {
    int64_t r = *(int64_t *)data->raw;
    number.ld = r;
    number.sign = 1;
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_BYTE: {
    uint8_t r = *(uint8_t *)data->raw;
    number.lu = r;
    number.sign = 1;
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t r = *(uint32_t *)data->raw;
    number.lu = r;
    number.sign = 0;
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t r = *(uint64_t *)data->raw;
    number.lu = r;
    number.sign = 0;
  } break;
  default:
    fprintf(stderr, "error: bad value type %d\n", kind);
    number.sign = -1;
  }
  return number;
}

uint64_t x86_64_number_unsigned(x86_64_number number) {
  if (number.sign < 0) {
    return UINT64_MAX;
  } else if (number.sign == 0) {
    return number.lu;
  } else {
    return number.ld;
  }
}

int64_t x86_64_number_signed(x86_64_number number) {
  if (number.sign < 0) {
    return INT64_MAX;
  } else if (number.sign == 0) {
    return number.lu;
  } else {
    return number.ld;
  }
}
