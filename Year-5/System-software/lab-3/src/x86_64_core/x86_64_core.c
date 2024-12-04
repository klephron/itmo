#include "x86_64_core.h"
#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define C_X86_64_VALUE(suffix, type_t, type_enum)                              \
  x86_64_value __x86_64_value_##suffix(type_t value);

#define C_X86_64_VALUE_DIRECT(suffix, type_t, type_enum)                       \
  x86_64_value __x86_64_value_##suffix(type_t value) {                         \
    assert(sizeof(type_t) == __x86_64_type_direct_size(type_enum));            \
    x86_64_data *data =                                                        \
        c_data_new(sizeof(type_t), (x86_64_type){.kind = type_enum});          \
    *data->raw = value;                                                        \
    return (x86_64_value){.data = data};                                       \
  }

typedef struct x86_64_number_struct {
  int8_t sign;
  union {
    int64_t  ld;
    uint64_t lu;
  };
} x86_64_number;

static x86_64_number c_get_number(x86_64_data *data) {
  x86_64_number number;

  switch (data->type.kind) {
  case X86_64_TYPE_INT: {
    int32_t r = *data->raw;
    number.ld = r;
    number.sign = 1;
  } break;
  case X86_64_TYPE_LONG: {
    int64_t r = *data->raw;
    number.ld = r;
    number.sign = 1;
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_BYTE: {
    uint8_t r = *data->raw;
    number.lu = r;
    number.sign = 1;
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t r = *data->raw;
    number.lu = r;
    number.sign = 0;
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t r = *data->raw;
    number.lu = r;
    number.sign = 0;
  } break;
  default:
    printf("error: bad value type %d\n", data->type.kind);
    number.sign = -1;
  }
  return number;
}

static inline uint64_t c_unsigned(x86_64_number number) {
  if (number.sign < 0) {
    return UINT64_MAX;
  } else if (number.sign == 0) {
    return number.lu;
  } else {
    return number.ld;
  }
}

static inline int64_t c_signed(x86_64_number number) {
  if (number.sign < 0) {
    return INT64_MAX;
  } else if (number.sign == 0) {
    return number.lu;
  } else {
    return number.ld;
  }
}

static int c_types_equals(x86_64_type *lsv, x86_64_type *rsv) {
  if (lsv->kind != rsv->kind) {
    return 0;
  }
  if (lsv->kind == X86_64_TYPE_ARRAY) {
    return c_types_equals(lsv->next, rsv->next);
  }
  return 1;
}

__attribute__((unused)) static void c_type_str(x86_64_type *type_ptr) {
  if (type_ptr) {
    printf("subtype: %d", type_ptr->kind);
    if (type_ptr->next) {
      printf(" -- ");
      c_type_str(type_ptr->next);
    }
  }
  printf("\n");
}

static x86_64_data *c_data_new(size_t data_size, x86_64_type type) {
  x86_64_data *data = malloc(sizeof(x86_64_data) + data_size);
  if (!data) {
    printf("error: malloc returned NULL\n");
    return NULL;
  }
  data->type = type;
  data->data_size = data_size;
  data->refs = 1;
  data->meta = NULL;
  return data;
}

static inline void c_type_free(x86_64_type *type) {
  x86_64_type *type_next;
  while (type) {
    type_next = type->next;
    free(type);
    type = type_next;
  }
}

static x86_64_type c_type_copy(const x86_64_type *type) {
  x86_64_type res;
  int         error = 0;

  assert(type != NULL);

  for (x86_64_type *res_cur = &res; type;
       type = type->next, res_cur = res_cur->next) {
    res_cur->kind = type->kind;

    if (type->next) {
      res_cur->next = (x86_64_type *)malloc(sizeof(x86_64_type));
      if (!res.next) {
        printf("error: malloc while copying type is NULL\n");
        error = 1;
        break;
      }
    } else {
      res_cur->next = NULL;
    }
  }

  if (error) {
    c_type_free(res.next);
  }

  return res;
}

size_t __x86_64_type_direct_size(x86_64_type_enum type) {
  switch (type) {
  case X86_64_TYPE_BOOL:
    return sizeof(uint8_t);
  case X86_64_TYPE_BYTE:
    return sizeof(uint8_t);
  case X86_64_TYPE_INT:
    return sizeof(int32_t);
  case X86_64_TYPE_UINT:
    return sizeof(uint32_t);
  case X86_64_TYPE_LONG:
    return sizeof(int64_t);
  case X86_64_TYPE_ULONG:
    return sizeof(uint64_t);
  case X86_64_TYPE_CHAR:
    return sizeof(char);
  case X86_64_TYPE_VOID:
    return 0;
    break;
  default:
    printf("error: type is only for direct types\n");
    return 0;
  }
}

void __x86_64_value_init(x86_64_value *ref) { ref->data = NULL; }

void __x86_64_value_drop(x86_64_value *ref) {
  x86_64_data *data = ref->data;
  if (!data) {
    return;
  }

  data->refs -= 1;
  if (data->refs) {
    return;
  }

  c_type_free(data->type.next);

  if (data->meta) {
    free(data->meta);
  }

  // free data
  free(data);
  ref->data = NULL;
}

// VALUE
x86_64_value __x86_64_value_void() { return (x86_64_value){.data = NULL}; }

C_X86_64_VALUE_DIRECT(bool, uint8_t, X86_64_TYPE_BOOL);
C_X86_64_VALUE_DIRECT(byte, uint8_t, X86_64_TYPE_BYTE);
C_X86_64_VALUE_DIRECT(int, int32_t, X86_64_TYPE_INT);
C_X86_64_VALUE_DIRECT(uint, uint32_t, X86_64_TYPE_UINT);
C_X86_64_VALUE_DIRECT(long, int64_t, X86_64_TYPE_LONG);
C_X86_64_VALUE_DIRECT(ulong, uint64_t, X86_64_TYPE_ULONG);
C_X86_64_VALUE_DIRECT(char, uint8_t, X86_64_TYPE_CHAR);

x86_64_value __x86_64_value_string(const uint8_t *value) {
  size_t       l = strlen((char *)value);
  x86_64_data *data =
      c_data_new(l + 1, (x86_64_type){.kind = X86_64_TYPE_STRING});
  memcpy(data->raw, value, l + 1);
  return (x86_64_value){.data = data};
}

x86_64_value __x86_64_value_array(x86_64_type *elem_type, uint64_t rank, ...) {
  assert(elem_type != NULL);

  va_list args;
  va_start(args, rank);

  uint64_t data_size = __x86_64_type_direct_size(elem_type->kind);

  x86_64_type type_root;
  type_root.next = NULL;

  x86_64_type *type_ptr = &type_root;

  x86_64_array_meta *meta =
      malloc(sizeof(x86_64_array_meta) + sizeof(meta->dimensions[0]) * rank);
  if (!meta) {
    printf("error: malloc\n");
    goto error;
  }

  meta->elem_size = data_size;
  meta->rank = rank;

  for (size_t i = 0; i < rank; type_ptr = type_ptr->next, ++i) {
    x86_64_value arg = va_arg(args, x86_64_value);
    if (arg.data == NULL) {
      printf("error: arg data is null\n");
      goto error;
    }

    uint64_t size = c_unsigned(c_get_number(arg.data));
    if (size == UINT64_MAX) {
      printf("error: with arg data %p while creating array\n", arg.data);
      goto error;
    }

    data_size *= size;
    meta->dimensions[i] = size;

    type_ptr->kind = X86_64_TYPE_ARRAY;
    type_ptr->next = malloc(sizeof(x86_64_type));
    if (!type_ptr->next) {
      printf("error: malloc returned NULL\n");
      goto error;
    }
  }

  *type_ptr = c_type_copy(elem_type);

  va_end(args);

  x86_64_data *data = c_data_new(data_size, type_root);
  data->meta_array = meta;

  return (x86_64_value){.data = data};

error:
  free(meta);
  c_type_free(type_root.next);
  va_end(args);
  return __x86_64_value_void();
}

// UNARY
x86_64_value __x86_64_plus(x86_64_value lsv) {
  if (!lsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __x86_64_value_byte(+l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __x86_64_value_int(+l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __x86_64_value_uint(+l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __x86_64_value_long(+l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __x86_64_value_ulong(+l);
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_minus(x86_64_value lsv) {
  if (!lsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __x86_64_value_byte(-l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __x86_64_value_int(-l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __x86_64_value_uint(-l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __x86_64_value_long(-l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __x86_64_value_ulong(-l);
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_not(x86_64_value lsv) {
  if (!lsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __x86_64_value_byte(!l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __x86_64_value_int(!l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __x86_64_value_uint(!l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __x86_64_value_long(!l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __x86_64_value_ulong(!l);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *data->raw;
    return __x86_64_value_bool(!l);
  } break;
  case X86_64_TYPE_CHAR:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_not(x86_64_value lsv) {
  if (!lsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __x86_64_value_byte(~l);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __x86_64_value_int(~l);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __x86_64_value_uint(~l);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __x86_64_value_long(~l);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __x86_64_value_ulong(~l);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *data->raw;
    return __x86_64_value_char(~l);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_BOOL:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_inc(x86_64_value lsv) {
  if (!lsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __x86_64_value_byte(l + 1);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __x86_64_value_int(l + 1);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __x86_64_value_uint(l + 1);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __x86_64_value_long(l + 1);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __x86_64_value_ulong(l + 1);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *data->raw;
    return __x86_64_value_char(l + 1);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_BOOL:
  default:
    printf("error: type implementation undefined\n");
    return __x86_64_value_void();
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_dec(x86_64_value lsv) {
  if (!lsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }
  x86_64_data *data = lsv.data;
  switch (data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __x86_64_value_byte(l - 1);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __x86_64_value_int(l - 1);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __x86_64_value_uint(l - 1);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __x86_64_value_long(l - 1);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __x86_64_value_ulong(l - 1);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *data->raw;
    return __x86_64_value_char(l - 1);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_BOOL:
  default:
    printf("error: type implementation undefined\n");
    return __x86_64_value_void();
  }
error:
  return __x86_64_value_void();
}

// BINARY
x86_64_value __x86_64_or(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l || r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_and(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l && r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_or(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l | r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l | r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l | r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l | r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l | r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_char(l | r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_xor(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l ^ r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l ^ r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l ^ r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l ^ r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l ^ r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_char(l ^ r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_and(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l & r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l & r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l & r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l & r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l & r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_char(l & r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_eq(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_BOOL: {
    uint8_t l = *lsv_data->raw;
    l = l != 0 ? 1 : 0;
    uint8_t r = *rsv_data->raw;
    r = r != 0 ? 1 : 0;
    return __x86_64_value_bool(l == r);
  } break;
  case X86_64_TYPE_STRING: {
    uint8_t *l = lsv_data->raw;
    uint8_t *r = rsv_data->raw;
    uint8_t  res = !strcmp((char *)l, (char *)r);
    return __x86_64_value_bool(res);
  } break;
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_neq(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_bool(l != r);
  case X86_64_TYPE_BOOL: {
    uint8_t l = *lsv_data->raw;
    l = l != 0 ? 1 : 0;
    uint8_t r = *rsv_data->raw;
    r = r != 0 ? 1 : 0;
    return __x86_64_value_bool(l != r);
  } break;
  case X86_64_TYPE_STRING: {
    uint8_t *l = lsv_data->raw;
    uint8_t *r = rsv_data->raw;
    uint8_t  res = strcmp((char *)l, (char *)r);
    return __x86_64_value_bool(res);
  } break;
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
  error:
    return __x86_64_value_void();
  }
}

x86_64_value __x86_64_less(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l < r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l < r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l < r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l < r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l < r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_char(l < r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_less_eq(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l <= r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l <= r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l <= r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l <= r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l <= r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_char(l <= r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_shl(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l << r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l << r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l << r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l << r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l << r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_char(l << r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_bit_shr(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l >> r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l >> r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l >> r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l >> r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l >> r);
  } break;
  case X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_char(l >> r);
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_add(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l + r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l + r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l + r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l + r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l + r);
  } break;
  case X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __x86_64_value_char(l + r);
  } break;
  case X86_64_TYPE_STRING: {
    uint8_t     *l = lsv_data->raw;
    uint8_t     *r = rsv_data->raw;
    x86_64_data *data =
        c_data_new(lsv_data->data_size + rsv_data->data_size - 1,
                   (x86_64_type){.kind = X86_64_TYPE_STRING});
    memcpy(data->raw, l, lsv_data->data_size - 1);
    memcpy(data->raw + lsv_data->data_size - 1, r, rsv_data->data_size - 1);
    data->raw[lsv_data->data_size + rsv_data->data_size - 2] = '\0';
    return (x86_64_value){.data = data};
  } break;
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_sub(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l - r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l - r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l - r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l - r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l - r);
  } break;
  case X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __x86_64_value_char(l - r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_mul(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l * r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l * r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l * r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l * r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l * r);
  } break;
  case X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __x86_64_value_char(l * r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_div(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l / r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l / r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l / r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l / r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l / r);
  } break;
  case X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __x86_64_value_char(l / r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

x86_64_value __x86_64_mod(x86_64_value lsv, x86_64_value rsv) {
  if (!lsv.data || !rsv.data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv.data;
  rsv_data = rsv.data;

  if (!c_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __x86_64_value_byte(l % r);
  } break;
  case X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __x86_64_value_int(l % r);
  } break;
  case X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __x86_64_value_uint(l % r);
  } break;
  case X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __x86_64_value_long(l % r);
  } break;
  case X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __x86_64_value_ulong(l % r);
  } break;
  case X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __x86_64_value_char(l % r);
  } break;
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_BOOL:
  case X86_64_TYPE_ARRAY:
  case X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __x86_64_value_void();
}

// SPECIAL
x86_64_value __x86_64_index_get(x86_64_value array, uint64_t dims, ...) {
  va_list args;
  va_start(args, dims);

  x86_64_data *array_data = array.data;

  if (!array_data) {
    printf("error: variable empty\n");
    goto error;
  }

  x86_64_data *lsv_data = array_data;

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_STRING: {
    if (dims != 1) {
      printf("error: multiple indexes aren't supported for this type\n");
      goto error;
    }

    x86_64_value arg = va_arg(args, x86_64_value);

    uint64_t index = c_unsigned(c_get_number(arg.data));
    if (index == UINT64_MAX) {
      printf("error: with arg data %p while creating array\n", arg.data);
      goto error;
    }

    va_end(args);

    char l = lsv_data->raw[index];

    return __x86_64_value_char(l);
  } break;
    // TODO: allow to get columns, not only elements
  case X86_64_TYPE_ARRAY: {
    x86_64_type       *type_ptr = &lsv_data->type;
    x86_64_array_meta *meta = lsv_data->meta_array;
    uint64_t           column_size = meta->elem_size;
    uint64_t           offset = 0;

    if (meta->rank != dims) {
      printf("error: array doesn't support slices\n");
      goto error;
    }

    for (uint64_t i = 0; i < dims; ++i, type_ptr = type_ptr->next) {
      x86_64_value arg = va_arg(args, x86_64_value);
      if (!arg.data) {
        printf("error: arg data %p no data\n", arg.data);
        goto error;
      }

      uint64_t index = c_unsigned(c_get_number(arg.data));
      if (index == UINT64_MAX) {
        printf("error: with arg data %p while creating array\n", arg.data);
        goto error;
      }

      offset += column_size * index;
      column_size *= meta->dimensions[dims - 1 - i];
    }

    va_end(args);

    x86_64_data *data = c_data_new(meta->elem_size, c_type_copy(type_ptr));
    memcpy(data->raw, array_data->raw + offset, sizeof(meta->elem_size));

    return (x86_64_value){.data = data};
  }
  default:
    printf("error: type implementation undefined\n");
  }
error:
  va_end(args);
  return __x86_64_value_void();
}

void __x86_64_index_set(x86_64_value from, x86_64_value array, uint64_t dims,
                        ...) {
  va_list args;
  va_start(args, dims);

  x86_64_data *from_data = from.data;
  x86_64_data *array_data = array.data;

  if (!from_data || !array_data) {
    printf("error: any variable empty\n");
    return;
  }

  switch (array_data->type.kind) {
  case X86_64_TYPE_STRING: {
    if (from.data->type.kind != X86_64_TYPE_CHAR) {
      printf("error: from should be char\n");
      goto error;
    }
    if (dims > 1) {
      printf("error: dims for string should be 1\n");
      goto error;
    }

    x86_64_value arg = va_arg(args, x86_64_value);
    if (!arg.data) {
      printf("error: arg data %p no data\n", arg.data);
      goto error;
    }

    uint64_t index = c_unsigned(c_get_number(arg.data));
    if (index == UINT64_MAX) {
      printf("error: with arg data %p while creating array\n", arg.data);
      goto error;
    }

    char l = *from_data->raw;
    array_data->raw[index] = l;

  } break;
  case X86_64_TYPE_ARRAY: {
    x86_64_type       *type_ptr = &array_data->type;
    x86_64_array_meta *meta = array_data->meta_array;
    uint64_t           column_size = meta->elem_size;
    uint64_t           offset = 0;

    if (meta->rank != dims) {
      printf("error: array doesn't support slices\n");
      goto error;
    }

    for (uint64_t i = 0; i < dims; ++i, type_ptr = type_ptr->next) {
      x86_64_value arg = va_arg(args, x86_64_value);
      if (!arg.data) {
        printf("error: arg data %p no data\n", arg.data);
        goto error;
      }

      uint64_t index = c_unsigned(c_get_number(arg.data));
      if (index == UINT64_MAX) {
        printf("error: with arg data %p while creating array\n", arg.data);
        goto error;
      }

      offset += column_size * index;
      column_size *= meta->dimensions[dims - 1 - i];
    }

    memcpy(array_data->raw + offset, from_data->raw, sizeof(meta->elem_size));

  } break;
  default:
    printf("error: type implementation undefined\n");
  }
error:
  va_end(args);
}

x86_64_value __x86_64_cast(x86_64_value lsv, x86_64_type_enum kind) {
  x86_64_data *lsv_data = lsv.data;

  if (!lsv_data) {
    printf("error: any variable empty\n");
    goto error;
  }

  x86_64_number num;

  switch (lsv_data->type.kind) {
  case X86_64_TYPE_BOOL: {
    num = c_get_number(lsv_data);
    break;
  }
  case X86_64_TYPE_BYTE: {
    num = c_get_number(lsv_data);
    break;
  }
  case X86_64_TYPE_INT: {
    num = c_get_number(lsv_data);
    break;
  }
  case X86_64_TYPE_UINT: {
    num = c_get_number(lsv_data);
    break;
  }
  case X86_64_TYPE_LONG: {
    num = c_get_number(lsv_data);
    break;
  }
  case X86_64_TYPE_ULONG: {
    num = c_get_number(lsv_data);
    break;
  }
  case X86_64_TYPE_CHAR: {
    num = c_get_number(lsv_data);
    break;
  }
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_ARRAY:
  default:
    printf("error: unsupported source cast value\n");
    goto error;
  }

  switch (kind) {
  case X86_64_TYPE_BOOL:
    return __x86_64_value_bool(c_unsigned(num));
  case X86_64_TYPE_BYTE:
    return __x86_64_value_byte(c_unsigned(num));
  case X86_64_TYPE_INT:
    return __x86_64_value_int(c_signed(num));
  case X86_64_TYPE_UINT:
    return __x86_64_value_uint(c_unsigned(num));
  case X86_64_TYPE_LONG:
    return __x86_64_value_long(c_signed(num));
  case X86_64_TYPE_ULONG:
    return __x86_64_value_ulong(c_unsigned(num));
  case X86_64_TYPE_CHAR:
    return __x86_64_value_char(c_unsigned(num));
  case X86_64_TYPE_VOID:
  case X86_64_TYPE_STRING:
  case X86_64_TYPE_ARRAY:
  default:
    printf("error: unsupported target cast value\n");
  }
error:
  return __x86_64_value_void();
}

// NOTE: no need to drop
void __x86_64_assign(x86_64_value *lsv, x86_64_value *rsv) {
  if (!rsv->data) {
    printf("error: assignment from null rsv\n");
  }
  if (lsv->data == rsv->data) {
    return;
  }
  __x86_64_value_drop(lsv);
  rsv->data->refs += 1;
  lsv->data = rsv->data;
}
