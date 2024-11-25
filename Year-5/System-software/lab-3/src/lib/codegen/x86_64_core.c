#include "x86_64_core.h"
#include <assert.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CG_X86_64_VALUE(suffix, type_t, type_enum)                             \
  cg_x86_64_value __cg_x86_64_value_##suffix(type_t value);

#define CG_X86_64_VALUE_DIRECT(suffix, type_t, type_enum)                      \
  cg_x86_64_value __cg_x86_64_value_##suffix(type_t value) {                   \
    assert(sizeof(type_t) == cg_type_direct_size(type_enum));                  \
    cg_x86_64_data *data =                                                     \
        cg_data_new(sizeof(type_t), (cg_x86_64_type){.kind = type_enum});      \
    *data->raw = value;                                                        \
    return (cg_x86_64_value){.data = data};                                    \
  }

typedef struct cg_x86_64_number_struct {
  int8_t sign;
  union {
    int64_t  ld;
    uint64_t lu;
  };
} cg_x86_64_number;

static cg_x86_64_number cg_get_number(cg_x86_64_data *data) {
  cg_x86_64_number number;

  switch (data->type.kind) {
  case CG_X86_64_TYPE_INT: {
    int32_t r = *data->raw;
    number.ld = r;
    number.sign = 1;
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t r = *data->raw;
    number.ld = r;
    number.sign = 1;
  } break;
  case CG_X86_64_TYPE_CHAR:
  case CG_X86_64_TYPE_BYTE: {
    uint8_t r = *data->raw;
    number.lu = r;
    number.sign = 1;
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t r = *data->raw;
    number.lu = r;
    number.sign = 0;
  } break;
  case CG_X86_64_TYPE_ULONG: {
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

static inline uint64_t cg_unsigned(cg_x86_64_number number) {
  if (number.sign < 0) {
    return UINT64_MAX;
  } else if (number.sign == 0) {
    return number.lu;
  } else {
    return number.ld;
  }
}

static inline int64_t cg_signed(cg_x86_64_number number) {
  if (number.sign < 0) {
    return INT64_MAX;
  } else if (number.sign == 0) {
    return number.lu;
  } else {
    return number.ld;
  }
}

static int cg_types_equals(cg_x86_64_type *lsv, cg_x86_64_type *rsv) {
  if (lsv->kind != rsv->kind) {
    return 0;
  }
  if (lsv->kind == CG_X86_64_TYPE_ARRAY) {
    return cg_types_equals(lsv->next, rsv->next);
  }
  return 1;
}

__attribute__((unused)) static void cg_type_str(cg_x86_64_type *type_ptr) {
  if (type_ptr) {
    printf("subtype: %d", type_ptr->kind);
    if (type_ptr->next) {
      printf(" -- ");
      cg_type_str(type_ptr->next);
    }
  }
  printf("\n");
}

static cg_x86_64_data *cg_data_new(size_t data_size, cg_x86_64_type type) {
  cg_x86_64_data *data = malloc(sizeof(cg_x86_64_data) + data_size);
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

static size_t cg_type_direct_size(cg_x86_64_type_enum type) {
  switch (type) {
  case CG_X86_64_TYPE_BOOL:
    return sizeof(uint8_t);
  case CG_X86_64_TYPE_BYTE:
    return sizeof(uint8_t);
  case CG_X86_64_TYPE_INT:
    return sizeof(int32_t);
  case CG_X86_64_TYPE_UINT:
    return sizeof(uint32_t);
  case CG_X86_64_TYPE_LONG:
    return sizeof(int64_t);
  case CG_X86_64_TYPE_ULONG:
    return sizeof(uint64_t);
  case CG_X86_64_TYPE_CHAR:
    return sizeof(char);
  case CG_X86_64_TYPE_VOID:
    return 0;
    break;
  default:
    printf("error: type is only for direct types\n");
    return 0;
  }
}

static inline void cg_type_free(cg_x86_64_type *type) {
  cg_x86_64_type *type_next;
  while (type) {
    type_next = type->next;
    free(type);
    type = type_next;
  }
}

static cg_x86_64_type cg_type_copy(const cg_x86_64_type *type) {
  cg_x86_64_type res;
  int            error = 0;

  assert(type != NULL);

  for (cg_x86_64_type *res_cur = &res; type;
       type = type->next, res_cur = res_cur->next) {
    res_cur->kind = type->kind;

    if (type->next) {
      res_cur->next = (cg_x86_64_type *)malloc(sizeof(cg_x86_64_type));
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
    cg_type_free(res.next);
  }

  return res;
}

void __cg_x86_64_init(cg_x86_64_value *ref) { ref->data = NULL; }

void __cg_x86_64_drop(cg_x86_64_value *ref) {
  cg_x86_64_data *data = ref->data;
  if (!data) {
    return;
  }

  data->refs -= 1;
  if (data->refs) {
    return;
  }

  cg_type_free(data->type.next);

  if (data->meta) {
    free(data->meta);
  }

  // free data
  free(data);
  ref->data = NULL;
}

// VALUE
cg_x86_64_value __cg_x86_64_value_void() {
  return (cg_x86_64_value){.data = NULL};
}

CG_X86_64_VALUE_DIRECT(bool, uint8_t, CG_X86_64_TYPE_BOOL);
CG_X86_64_VALUE_DIRECT(byte, uint8_t, CG_X86_64_TYPE_BYTE);
CG_X86_64_VALUE_DIRECT(int, int32_t, CG_X86_64_TYPE_INT);
CG_X86_64_VALUE_DIRECT(uint, uint32_t, CG_X86_64_TYPE_UINT);
CG_X86_64_VALUE_DIRECT(long, int64_t, CG_X86_64_TYPE_LONG);
CG_X86_64_VALUE_DIRECT(ulong, uint64_t, CG_X86_64_TYPE_ULONG);
CG_X86_64_VALUE_DIRECT(char, uint8_t, CG_X86_64_TYPE_CHAR);

cg_x86_64_value __cg_x86_64_value_string(const uint8_t *value) {
  size_t          l = strlen((char *)value);
  cg_x86_64_data *data =
      cg_data_new(l + 1, (cg_x86_64_type){.kind = CG_X86_64_TYPE_STRING});
  memcpy(data->raw, value, l + 1);
  return (cg_x86_64_value){.data = data};
}

cg_x86_64_value __cg_x86_64_value_array(cg_x86_64_type *elem_type,
                                        uint64_t        rank, ...) {
  assert(elem_type != NULL);

  va_list args;
  va_start(args, rank);

  uint64_t data_size = cg_type_direct_size(elem_type->kind);

  cg_x86_64_type type_root;
  type_root.next = NULL;

  cg_x86_64_type *type_ptr = &type_root;

  cg_x86_64_array_meta *meta =
      malloc(sizeof(cg_x86_64_array_meta) + sizeof(meta->dimensions[0]) * rank);
  if (!meta) {
    printf("error: malloc\n");
    goto error;
  }

  meta->elem_size = data_size;
  meta->rank = rank;

  for (size_t i = 0; i < rank; type_ptr = type_ptr->next, ++i) {
    cg_x86_64_value *arg = va_arg(args, cg_x86_64_value *);
    if (arg->data == NULL) {
      printf("error: arg data is null\n");
      goto error;
    }

    uint64_t size = cg_unsigned(cg_get_number(arg->data));
    if (size == UINT64_MAX) {
      printf("error: with arg %p while creating array\n", arg);
      goto error;
    }

    data_size *= size;
    meta->dimensions[i] = size;

    type_ptr->kind = CG_X86_64_TYPE_ARRAY;
    type_ptr->next = malloc(sizeof(cg_x86_64_type));
    if (!type_ptr->next) {
      printf("error: malloc returned NULL\n");
      goto error;
    }
  }

  *type_ptr = cg_type_copy(elem_type);

  va_end(args);

  cg_x86_64_data *data = cg_data_new(data_size, type_root);
  data->meta_array = meta;

  return (cg_x86_64_value){.data = data};

error:
  free(meta);
  cg_type_free(type_root.next);
  va_end(args);
  return __cg_x86_64_value_void();
}

// UNARY
cg_x86_64_value __cg_x86_64_plus(cg_x86_64_value *lsv) {
  if (!lsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }
  cg_x86_64_data *data = lsv->data;
  switch (data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_byte(+l);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __cg_x86_64_value_int(+l);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __cg_x86_64_value_uint(+l);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __cg_x86_64_value_long(+l);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __cg_x86_64_value_ulong(+l);
  } break;
  case CG_X86_64_TYPE_CHAR:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_minus(cg_x86_64_value *lsv) {
  if (!lsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }
  cg_x86_64_data *data = lsv->data;
  switch (data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_byte(-l);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __cg_x86_64_value_int(-l);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __cg_x86_64_value_uint(-l);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __cg_x86_64_value_long(-l);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __cg_x86_64_value_ulong(-l);
  } break;
  case CG_X86_64_TYPE_CHAR:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_not(cg_x86_64_value *lsv) {
  if (!lsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }
  cg_x86_64_data *data = lsv->data;
  switch (data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_byte(!l);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __cg_x86_64_value_int(!l);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __cg_x86_64_value_uint(!l);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __cg_x86_64_value_long(!l);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __cg_x86_64_value_ulong(!l);
  } break;
  case CG_X86_64_TYPE_BOOL: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_bool(!l);
  } break;
  case CG_X86_64_TYPE_CHAR:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_bit_not(cg_x86_64_value *lsv) {
  if (!lsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }
  cg_x86_64_data *data = lsv->data;
  switch (data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_byte(~l);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __cg_x86_64_value_int(~l);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __cg_x86_64_value_uint(~l);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __cg_x86_64_value_long(~l);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __cg_x86_64_value_ulong(~l);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_char(~l);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  case CG_X86_64_TYPE_BOOL:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_inc(cg_x86_64_value *lsv) {
  if (!lsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }
  cg_x86_64_data *data = lsv->data;
  switch (data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_byte(l + 1);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __cg_x86_64_value_int(l + 1);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __cg_x86_64_value_uint(l + 1);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __cg_x86_64_value_long(l + 1);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __cg_x86_64_value_ulong(l + 1);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_char(l + 1);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  case CG_X86_64_TYPE_BOOL:
  default:
    printf("error: type implementation undefined\n");
    return __cg_x86_64_value_void();
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_dec(cg_x86_64_value *lsv) {
  if (!lsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }
  cg_x86_64_data *data = lsv->data;
  switch (data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_byte(l - 1);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *data->raw;
    return __cg_x86_64_value_int(l - 1);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *data->raw;
    return __cg_x86_64_value_uint(l - 1);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *data->raw;
    return __cg_x86_64_value_long(l - 1);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *data->raw;
    return __cg_x86_64_value_ulong(l - 1);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *data->raw;
    return __cg_x86_64_value_char(l - 1);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  case CG_X86_64_TYPE_BOOL:
  default:
    printf("error: type implementation undefined\n");
    return __cg_x86_64_value_void();
  }
error:
  return __cg_x86_64_value_void();
}

// BINARY
cg_x86_64_value __cg_x86_64_or(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l || r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l || r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l || r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l || r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l || r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l || r);
  } break;
  case CG_X86_64_TYPE_BOOL: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l || r);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_and(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l && r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l && r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l && r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l && r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l && r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l && r);
  } break;
  case CG_X86_64_TYPE_BOOL: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l && r);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_bit_or(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l | r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l | r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l | r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l | r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l | r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_char(l | r);
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_bit_xor(cg_x86_64_value *lsv,
                                    cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l ^ r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l ^ r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l ^ r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l ^ r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l ^ r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_char(l ^ r);
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_bit_and(cg_x86_64_value *lsv,
                                    cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l & r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l & r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l & r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l & r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l & r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_char(l & r);
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_eq(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l == r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l == r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l == r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l == r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l == r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l == r);
  } break;
  case CG_X86_64_TYPE_BOOL: {
    uint8_t l = *lsv_data->raw;
    l = l != 0 ? 1 : 0;
    uint8_t r = *rsv_data->raw;
    r = r != 0 ? 1 : 0;
    return __cg_x86_64_value_bool(l == r);
  } break;
  case CG_X86_64_TYPE_STRING: {
    uint8_t *l = lsv_data->raw;
    uint8_t *r = rsv_data->raw;
    uint8_t  res = !strcmp((char *)l, (char *)r);
    return __cg_x86_64_value_bool(res);
  } break;
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_neq(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l != r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l != r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l != r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l != r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l != r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_bool(l != r);
  } break;
    uint8_t l = *lsv_data->raw;
    l = l != 0 ? 1 : 0;
    uint8_t r = *rsv_data->raw;
    r = r != 0 ? 1 : 0;
    return __cg_x86_64_value_bool(l != r);
  case CG_X86_64_TYPE_STRING: {
    uint8_t *l = lsv_data->raw;
    uint8_t *r = rsv_data->raw;
    uint8_t  res = strcmp((char *)l, (char *)r);
    return __cg_x86_64_value_bool(res);
  } break;
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_less(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l < r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l < r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l < r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l < r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l < r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_char(l < r);
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_less_eq(cg_x86_64_value *lsv,
                                    cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l <= r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l <= r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l <= r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l <= r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l <= r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_char(l <= r);
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_bit_shl(cg_x86_64_value *lsv,
                                    cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l << r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l << r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l << r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l << r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l << r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_char(l << r);
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_bit_shr(cg_x86_64_value *lsv,
                                    cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l >> r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l >> r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l >> r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l >> r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l >> r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_char(l >> r);
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_add(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l + r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l + r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l + r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l + r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l + r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __cg_x86_64_value_char(l + r);
  } break;
  case CG_X86_64_TYPE_STRING: {
    uint8_t        *l = lsv_data->raw;
    uint8_t        *r = rsv_data->raw;
    cg_x86_64_data *data =
        cg_data_new(lsv_data->data_size + rsv_data->data_size - 1,
                    (cg_x86_64_type){.kind = CG_X86_64_TYPE_STRING});
    memcpy(data->raw, l, lsv_data->data_size - 1);
    memcpy(data->raw + lsv_data->data_size - 1, r, rsv_data->data_size - 1);
    data->raw[lsv_data->data_size + rsv_data->data_size - 2] = '\0';
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_sub(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l - r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l - r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l - r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l - r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l - r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __cg_x86_64_value_char(l - r);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_mul(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l * r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l * r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l * r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l * r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l * r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __cg_x86_64_value_char(l * r);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_div(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l / r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l / r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l / r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l / r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l / r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __cg_x86_64_value_char(l / r);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

cg_x86_64_value __cg_x86_64_mod(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!cg_types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("error: types mismatch\n");
    goto error;
  }

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t l = *lsv_data->raw;
    uint8_t r = *rsv_data->raw;
    return __cg_x86_64_value_byte(l % r);
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t l = *lsv_data->raw;
    int32_t r = *rsv_data->raw;
    return __cg_x86_64_value_int(l % r);
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t l = *lsv_data->raw;
    uint32_t r = *rsv_data->raw;
    return __cg_x86_64_value_uint(l % r);
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t l = *lsv_data->raw;
    int64_t r = *rsv_data->raw;
    return __cg_x86_64_value_long(l % r);
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t l = *lsv_data->raw;
    uint64_t r = *rsv_data->raw;
    return __cg_x86_64_value_ulong(l % r);
  } break;
  case CG_X86_64_TYPE_CHAR: {
    char l = *lsv_data->raw;
    char r = *rsv_data->raw;
    return __cg_x86_64_value_char(l % r);
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("error: type implementation undefined\n");
  }
error:
  return __cg_x86_64_value_void();
}

// SPECIAL
cg_x86_64_value __cg_x86_64_index(cg_x86_64_value *array, uint64_t dims, ...) {
  va_list args;
  va_start(args, dims);

  if (!array->data) {
    printf("error: variable empty\n");
    goto error;
  }

  cg_x86_64_data *lsv_data = array->data;

  switch (lsv_data->type.kind) {
  case CG_X86_64_TYPE_STRING: {
    if (dims != 1) {
      printf("error: multiple indexes aren't supported for this type\n");
      goto error;
    }

    cg_x86_64_value *arg = va_arg(args, cg_x86_64_value *);

    uint64_t index = cg_unsigned(cg_get_number(arg->data));
    if (index == UINT64_MAX) {
      printf("error: with arg %p while creating array\n", arg);
      goto error;
    }

    va_end(args);

    char l = lsv_data->raw[index];

    return __cg_x86_64_value_char(l);
  } break;
    // TODO: allow to get columns, not only elements
  case CG_X86_64_TYPE_ARRAY: {
    cg_x86_64_type       *type_ptr = &lsv_data->type;
    cg_x86_64_array_meta *meta = lsv_data->meta_array;
    uint64_t              column_size = meta->elem_size;
    uint64_t              offset = 0;

    if (meta->rank != dims) {
      printf("error: array doesn't support slices\n");
      goto error;
    }

    for (uint64_t i = 0; i < dims; ++i, type_ptr = type_ptr->next) {
      cg_x86_64_value *arg = va_arg(args, cg_x86_64_value *);
      if (!arg->data) {
        printf("error: arg %p no data\n", arg);
        goto error;
      }

      uint64_t index = cg_unsigned(cg_get_number(arg->data));
      if (index == UINT64_MAX) {
        printf("error: with arg %p while creating array\n", arg);
        goto error;
      }

      offset += column_size * index;
      column_size *= meta->dimensions[dims - 1 - i];
    }

    va_end(args);

    cg_x86_64_data *data = cg_data_new(meta->elem_size, cg_type_copy(type_ptr));
    memcpy(data->raw, array->data->raw + offset, sizeof(meta->elem_size));

    return (cg_x86_64_value){.data = data};
  }
  default:
    printf("error: type implementation undefined\n");
  }
error:
  va_end(args);
  return __cg_x86_64_value_void();
}

void __cg_x86_64_index_set(cg_x86_64_value *from, cg_x86_64_value *array,
                           uint64_t dims, ...) {
  va_list args;
  va_start(args, dims);

  if (!from->data || !array->data) {
    printf("error: any variable empty\n");
    return;
  }

  switch (array->data->type.kind) {
  case CG_X86_64_TYPE_STRING: {
    if (from->data->type.kind != CG_X86_64_TYPE_CHAR) {
      printf("error: from should be char\n");
      goto error;
    }
    if (dims > 1) {
      printf("error: dims for string should be 1\n");
      goto error;
    }

    cg_x86_64_value *arg = va_arg(args, cg_x86_64_value *);
    if (!arg->data) {
      printf("error: arg %p no data\n", arg);
      goto error;
    }

    uint64_t index = cg_unsigned(cg_get_number(arg->data));
    if (index == UINT64_MAX) {
      printf("error: with arg %p while creating array\n", arg);
      goto error;
    }

    char l = *from->data->raw;
    array->data->raw[index] = l;

  } break;
  case CG_X86_64_TYPE_ARRAY: {
    cg_x86_64_data *array_data = array->data;

    cg_x86_64_type       *type_ptr = &array_data->type;
    cg_x86_64_array_meta *meta = array_data->meta_array;
    uint64_t              column_size = meta->elem_size;
    uint64_t              offset = 0;

    if (meta->rank != dims) {
      printf("error: array doesn't support slices\n");
      goto error;
    }

    for (uint64_t i = 0; i < dims; ++i, type_ptr = type_ptr->next) {
      cg_x86_64_value *arg = va_arg(args, cg_x86_64_value *);
      if (!arg->data) {
        printf("error: arg %p no data\n", arg);
        goto error;
      }

      uint64_t index = cg_unsigned(cg_get_number(arg->data));
      if (index == UINT64_MAX) {
        printf("error: with arg %p while creating array\n", arg);
        goto error;
      }

      offset += column_size * index;
      column_size *= meta->dimensions[dims - 1 - i];
    }

    memcpy(array_data->raw + offset, from->data->raw, sizeof(meta->elem_size));

  } break;
  default:
    printf("error: type implementation undefined\n");
  }
error:
  va_end(args);
}

cg_x86_64_value __cg_x86_64_cast(cg_x86_64_value    *lsv,
                                 cg_x86_64_type_enum kind) {
  if (!lsv->data) {
    printf("error: any variable empty\n");
    goto error;
  }

  cg_x86_64_number num;

  switch (lsv->data->type.kind) {
  case CG_X86_64_TYPE_BOOL: {
    num = cg_get_number(lsv->data);
    break;
  }
  case CG_X86_64_TYPE_BYTE: {
    num = cg_get_number(lsv->data);
    break;
  }
  case CG_X86_64_TYPE_INT: {
    num = cg_get_number(lsv->data);
    break;
  }
  case CG_X86_64_TYPE_UINT: {
    num = cg_get_number(lsv->data);
    break;
  }
  case CG_X86_64_TYPE_LONG: {
    num = cg_get_number(lsv->data);
    break;
  }
  case CG_X86_64_TYPE_ULONG: {
    num = cg_get_number(lsv->data);
    break;
  }
  case CG_X86_64_TYPE_CHAR: {
    num = cg_get_number(lsv->data);
    break;
  }
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_VOID:
  case CG_X86_64_TYPE_ARRAY:
  default:
    printf("error: unsupported source cast value\n");
    goto error;
  }

  switch (kind) {
  case CG_X86_64_TYPE_BOOL:
    return __cg_x86_64_value_bool(cg_unsigned(num));
  case CG_X86_64_TYPE_BYTE:
    return __cg_x86_64_value_byte(cg_unsigned(num));
  case CG_X86_64_TYPE_INT:
    return __cg_x86_64_value_int(cg_signed(num));
  case CG_X86_64_TYPE_UINT:
    return __cg_x86_64_value_uint(cg_unsigned(num));
  case CG_X86_64_TYPE_LONG:
    return __cg_x86_64_value_long(cg_signed(num));
  case CG_X86_64_TYPE_ULONG:
    return __cg_x86_64_value_ulong(cg_unsigned(num));
  case CG_X86_64_TYPE_CHAR:
    return __cg_x86_64_value_char(cg_unsigned(num));
  case CG_X86_64_TYPE_VOID:
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_ARRAY:
  default:
    printf("error: unsupported target cast value\n");
  }
error:
  return __cg_x86_64_value_void();
}
