#include "operation.h"
#include "x86_64_core/number.h"

#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// // UNARY
x86_64_value __x86_64_plus(x86_64_value lsv) {
  if (!lsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (x86_64_type_kind(data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_byte(+l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)data->raw;
    return __x86_64_value_int(+l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)data->raw;
    return __x86_64_value_uint(+l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)data->raw;
    return __x86_64_value_long(+l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)data->raw;
    return __x86_64_value_ulong(+l);
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_minus(x86_64_value lsv) {
  if (!lsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (x86_64_type_kind(data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_byte(-l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)data->raw;
    return __x86_64_value_int(-l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)data->raw;
    return __x86_64_value_uint(-l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)data->raw;
    return __x86_64_value_long(-l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)data->raw;
    return __x86_64_value_ulong(-l);
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_not(x86_64_value lsv) {
  if (!lsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (x86_64_type_kind(data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_byte(!l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)data->raw;
    return __x86_64_value_int(!l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)data->raw;
    return __x86_64_value_uint(!l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)data->raw;
    return __x86_64_value_long(!l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)data->raw;
    return __x86_64_value_ulong(!l);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_bool(!l);
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_not(x86_64_value lsv) {
  if (!lsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (x86_64_type_kind(data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_byte(~l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)data->raw;
    return __x86_64_value_int(~l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)data->raw;
    return __x86_64_value_uint(~l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)data->raw;
    return __x86_64_value_long(~l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)data->raw;
    return __x86_64_value_ulong(~l);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_char(~l);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_BOOL:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_inc(x86_64_value lsv) {
  if (!lsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (x86_64_type_kind(data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_byte(l + 1);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)data->raw;
    return __x86_64_value_int(l + 1);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)data->raw;
    return __x86_64_value_uint(l + 1);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)data->raw;
    return __x86_64_value_long(l + 1);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)data->raw;
    return __x86_64_value_ulong(l + 1);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_char(l + 1);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_BOOL:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
    return __x86_64_value_void();
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_dec(x86_64_value lsv) {
  if (!lsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (x86_64_type_kind(data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_byte(l - 1);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)data->raw;
    return __x86_64_value_int(l - 1);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)data->raw;
    return __x86_64_value_uint(l - 1);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)data->raw;
    return __x86_64_value_long(l - 1);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)data->raw;
    return __x86_64_value_ulong(l - 1);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)data->raw;
    return __x86_64_value_char(l - 1);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_BOOL:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
    return __x86_64_value_void();
  }
error:
  return __x86_64_value_void();
}

// BINARY
x86_64_value __x86_64_or(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_and(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_or(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l | r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l | r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l | r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l | r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l | r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l | r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_xor(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l ^ r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l ^ r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l ^ r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l ^ r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l ^ r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l ^ r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_and(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l & r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l & r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l & r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l & r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l & r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l & r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_eq(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    l = l != 0 ? 1 : 0;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    r = r != 0 ? 1 : 0;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_STRING: {
    uint8_t *l = (uint8_t *)lsv_data->raw;
    uint8_t *r = (uint8_t *)rsv_data->raw;
    uint8_t  res = !strcmp((char *)l, (char *)r);
    return __x86_64_value_bool(res);
  } break;
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_neq(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l != r);
  case X86_64_TYPE_BOOL: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    l = l != 0 ? 1 : 0;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    r = r != 0 ? 1 : 0;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_STRING: {
    uint8_t *l = (uint8_t *)lsv_data->raw;
    uint8_t *r = (uint8_t *)rsv_data->raw;
    uint8_t  res = strcmp((char *)l, (char *)r);
    return __x86_64_value_bool(res);
  } break;
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
  error:
    return __x86_64_value_void();
  }
}

x86_64_value __x86_64_less(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l < r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_bool(l < r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_bool(l < r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_bool(l < r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_bool(l < r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l < r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_less_eq(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l <= r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_bool(l <= r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_bool(l <= r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_bool(l <= r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_bool(l <= r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_bool(l <= r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_shl(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l << r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l << r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l << r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l << r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l << r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l << r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_shr(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l >> r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l >> r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l >> r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l >> r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l >> r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l >> r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_add(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l + r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l + r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l + r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l + r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l + r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l + r);
  } break;
  case X86_64_TYPE_STRING: {
    uint8_t     *l = (uint8_t *)lsv_data->raw;
    uint8_t     *r = (uint8_t *)rsv_data->raw;
    x86_64_data *data =
        x86_64_data_new(lsv_data->raw_size + rsv_data->raw_size - 1,
                        x86_64_type_new_simple(X86_64_TYPE_STRING));
    memcpy(data->raw, l, lsv_data->raw_size - 1);
    memcpy(data->raw + lsv_data->raw_size - 1, r, rsv_data->raw_size - 1);
    data->raw[lsv_data->raw_size + rsv_data->raw_size - 2] = '\0';
    return (x86_64_value){.data = data};
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_sub(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l - r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l - r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l - r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l - r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l - r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l - r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_mul(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l * r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l * r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l * r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l * r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l * r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l * r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_div(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l / r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l / r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l / r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l / r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l / r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l / r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_mod(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!x86_64_type_equals(lsv_data->type, rsv_data->type)) {
    fprintf(stderr, "error: types mismatch\n");
    goto error;
  }

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_byte(l % r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *(int32_t *)lsv_data->raw;
    int32_t r = *(int32_t *)rsv_data->raw;
    return __x86_64_value_int(l % r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *(uint32_t *)lsv_data->raw;
    uint32_t r = *(uint32_t *)rsv_data->raw;
    return __x86_64_value_uint(l % r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *(int64_t *)lsv_data->raw;
    int64_t r = *(int64_t *)rsv_data->raw;
    return __x86_64_value_long(l % r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *(uint64_t *)lsv_data->raw;
    uint64_t r = *(uint64_t *)rsv_data->raw;
    return __x86_64_value_ulong(l % r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *(uint8_t *)lsv_data->raw;
    uint8_t r = *(uint8_t *)rsv_data->raw;
    return __x86_64_value_char(l % r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_index_get(x86_64_value array, ...) {
  va_list args;
  va_start(args, array);

  x86_64_data *array_data = array.data;

  if (!array_data) {
    fprintf(stderr, "error: variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data = array_data;

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_STRING: {
    x86_64_value arg = va_arg(args, x86_64_value);

    uint64_t index = x86_64_number_unsigned(x86_64_number_get(arg.data));
    if (index == UINT64_MAX) {
      fprintf(stderr, "error: with arg data %p while creating array\n",
              arg.data);
      goto error;
    }

    va_end(args);

    uint8_t l = lsv_data->raw[index];

    return __x86_64_value_char(l);
  } break;
    // TODO: allow to get columns, not only elements
  case X86_64_TYPE_ARRAY: {
    x86_64_type *type = lsv_data->type;
    uint64_t     offset = 0;
    uint64_t     entry_size = 0;
    uint64_t     i = 0;

    while (1) {
      x86_64_value arg = va_arg(args, x86_64_value);
      if (!arg.data) {
        break;
      }

      uint64_t index = x86_64_number_unsigned(x86_64_number_get(arg.data));
      if (index == UINT64_MAX) {
        fprintf(stderr, "error: with arg data %p while creating array\n",
                arg.data);
        goto error;
      }

      entry_size = type->entries[i].meta_array->size;
      offset += entry_size * index;
      ++i;
    }

    va_end(args);

    x86_64_data *data = x86_64_data_new(
        entry_size,
        x86_64_type_copy_u_entry(type->count, i, type->entries + i));
    memcpy(data->raw, array_data->raw + offset, entry_size);

    return (x86_64_value){.data = data};
  }
  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }
error:
  va_end(args);
  return __x86_64_value_void();
}

x86_64_value __x86_64_index_set(x86_64_value from, x86_64_value array, ...) {
  va_list args;
  va_start(args, array);

  x86_64_data *from_data = from.data;
  x86_64_data *array_data = array.data;

  if (!from_data || !array_data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  switch (x86_64_type_kind(array_data->type)) {
  case X86_64_TYPE_STRING: {
    if (x86_64_type_kind(from_data->type) != X86_64_TYPE_CHAR) {
      fprintf(stderr, "error: from should be char\n");
      goto error;
    }

    x86_64_value arg = va_arg(args, x86_64_value);
    if (!arg.data) {
      fprintf(stderr, "error: arg data %p no data\n", arg.data);
      goto error;
    }

    uint64_t index = x86_64_number_unsigned(x86_64_number_get(arg.data));
    if (index == UINT64_MAX) {
      fprintf(stderr, "error: with arg data %p while creating array\n",
              arg.data);
      goto error;
    }

    char l = *from_data->raw;
    array_data->raw[index] = l;
    return from;
  } break;
  case X86_64_TYPE_ARRAY: {
    x86_64_type *type = array_data->type;
    uint64_t     entry_size = 0;
    uint64_t     offset = 0;
    uint64_t     i = 0;

    while (1) {
      x86_64_value arg = va_arg(args, x86_64_value);
      if (!arg.data) {
        break;
      }

      uint64_t index = x86_64_number_unsigned(x86_64_number_get(arg.data));
      if (index == UINT64_MAX) {
        fprintf(stderr, "error: with arg data %p while creating array\n",
                arg.data);
        goto error;
      }

      entry_size = type->entries[i].meta_array->size;
      offset += entry_size * index;
      ++i;
    }

    memcpy(array_data->raw + offset, from_data->raw, entry_size);
    return from;
  } break;

  default:
    fprintf(stderr, "error: type implementation undefined\n");
  }

error:
  va_end(args);
  return __x86_64_value_void();
}

x86_64_value __x86_64_cast(x86_64_value lsv, x86_64_type_enum kind) {
  x86_64_data *lsv_data = lsv.data;

  if (!lsv_data) {
    fprintf(stderr, "error: any variable empty\n");
    goto error;
  }

  x86_64_number num;

  switch (x86_64_type_kind(lsv_data->type)) {
  case X86_64_TYPE_BOOL: {
    num = x86_64_number_get(lsv_data);
    break;
  }
  case X86_64_TYPE_BYTE: {
    num = x86_64_number_get(lsv_data);
    break;
  }
  case X86_64_TYPE_INT: {
    num = x86_64_number_get(lsv_data);
    break;
  }
  case X86_64_TYPE_UINT: {
    num = x86_64_number_get(lsv_data);
    break;
  }
  case X86_64_TYPE_LONG: {
    num = x86_64_number_get(lsv_data);
    break;
  }
  case X86_64_TYPE_ULONG: {
    num = x86_64_number_get(lsv_data);
    break;
  }
  case X86_64_TYPE_CHAR: {
    num = x86_64_number_get(lsv_data);
    break;
  }
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_ARRAY:
  default:
    fprintf(stderr, "error: unsupported source cast value\n");
    goto error;
  }

  switch (kind) {
  case X86_64_TYPE_BOOL:
    return __x86_64_value_bool(x86_64_number_unsigned(num));
  case X86_64_TYPE_BYTE:
    return __x86_64_value_byte(x86_64_number_unsigned(num));
  case X86_64_TYPE_INT:
    return __x86_64_value_int(x86_64_number_signed(num));
  case X86_64_TYPE_UINT:
    return __x86_64_value_uint(x86_64_number_unsigned(num));
  case X86_64_TYPE_LONG:
    return __x86_64_value_long(x86_64_number_signed(num));
  case X86_64_TYPE_ULONG:
    return __x86_64_value_ulong(x86_64_number_unsigned(num));
  case X86_64_TYPE_CHAR:
    return __x86_64_value_char(x86_64_number_unsigned(num));
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  default:
    fprintf(stderr, "error: unsupported target cast value\n");
  }
error:
  return __x86_64_value_void();
}

// NOTE: assigment from __x86_64_value_void() is same as drop
void __x86_64_assign(x86_64_value *lsv, x86_64_value rsv) {
  if (lsv->data == rsv.data) {
    return;
  }
  __x86_64_value_drop(lsv);
  if (rsv.data) {
    rsv.data->refs += 1;
  }
  lsv->data = rsv.data;
}

x86_64_value __x86_64_print(x86_64_value value) {
  x86_64_data *data = value.data;

  const char *end = "\n";

  if (!data) {
    fprintf(stdout, "<empty>");
    goto print_end;
  }

  switch (x86_64_type_kind(data->type)) {
  case X86_64_TYPE_VOID:
    printf("<void>");
    break;
  case X86_64_TYPE_BOOL:
    if (__x86_64_unwrap_bool(value)) {
      fprintf(stdout, "true");
    } else {
      fprintf(stdout, "false");
    }
    break;
  case X86_64_TYPE_BYTE:
    fprintf(stdout, "%u", __x86_64_unwrap_byte(value));
    break;
  case X86_64_TYPE_INT:
    fprintf(stdout, "%d", __x86_64_unwrap_int(value));
    break;
  case X86_64_TYPE_UINT:
    fprintf(stdout, "%d", __x86_64_unwrap_uint(value));
    break;
  case X86_64_TYPE_LONG:
    fprintf(stdout, "%ld", __x86_64_unwrap_long(value));
    break;
  case X86_64_TYPE_ULONG:
    fprintf(stdout, "%lu", __x86_64_unwrap_ulong(value));
    break;
  case X86_64_TYPE_CHAR:
    fprintf(stdout, "%c", __x86_64_unwrap_char(value));
    break;
  case X86_64_TYPE_STRING:
    fprintf(stdout, "%s", __x86_64_unwrap_string(value));
    break;
  case X86_64_TYPE_ARRAY:
    fprintf(stderr, "warn: array printing is unsupported\n");
    fprintf(stdout, "<array>");
    break;
  default:
    fprintf(stderr, "error: unknown type %d\n", x86_64_type_kind(data->type));
    fprintf(stdout, "<unknown>");
  }

print_end:
  fprintf(stdout, "%s", end);
  return __x86_64_value_void();
}

x86_64_value __x86_64_fflush() {
  fflush(NULL);
  return __x86_64_value_void();
}
