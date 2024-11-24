#include "x86_64_core.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int types_equals(cg_x86_64_type *lsv, cg_x86_64_type *rsv) {
  if (lsv->type != rsv->type) {
    return 0;
  }
  if (lsv->type == CG_X86_64_TYPE_ARRAY) {
    return types_equals(lsv->next, rsv->next);
  }
  return 1;
}

static cg_x86_64_data *data_new(size_t data_size, cg_x86_64_type type) {
  cg_x86_64_data *data = malloc(sizeof(cg_x86_64_data) + data_size);
  if (!data) {
    printf("malloc returned NULL\n");
    return NULL;
  }
  data->type = type;
  data->data_size = data_size;
  data->refs = 1;
  return data;
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

  // free recursive type structure
  cg_x86_64_type *type = data->type.next;
  cg_x86_64_type *type_next;
  if (type) {
    type_next = type->next;
    free(type);
    type = type_next;
  }

  // free data
  free(data);
  ref->data = NULL;
}

cg_x86_64_value __cg_x86_64_value_bool(int8_t value);
cg_x86_64_value __cg_x86_64_value_byte(uint8_t value);

cg_x86_64_value __cg_x86_64_value_int(int32_t value) {
  cg_x86_64_data *data =
      data_new(sizeof(int32_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_INT});
  *data->data = value;
  return (cg_x86_64_value){.data = data};
}

cg_x86_64_value __cg_x86_64_value_uint(uint32_t value);
cg_x86_64_value __cg_x86_64_value_long(int64_t value);
cg_x86_64_value __cg_x86_64_value_ulong(uint64_t value);
cg_x86_64_value __cg_x86_64_value_char(char value);

cg_x86_64_value __cg_x86_64_value_string(const char *value) {
  size_t          l = strlen(value);
  cg_x86_64_data *data =
      data_new(l + 1, (cg_x86_64_type){.type = CG_X86_64_TYPE_STRING});
  memcpy(data->data, value, l + 1);
  return (cg_x86_64_value){.data = data};
}

cg_x86_64_value __cg_x86_64_value_array(cg_x86_64_type *type, size_t dims, ...);

cg_x86_64_value __cg_x86_64_plus(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_minus(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_not(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_bit_not(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_inc(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_dec(cg_x86_64_value *lsv);

cg_x86_64_value __cg_x86_64_or(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_and(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_bit_or(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_bit_xor(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_bit_and(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_eq(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_neq(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_less(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_less_eq(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_bit_shl(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_bit_shr(cg_x86_64_value *lsv, cg_x86_64_value *rsv);

cg_x86_64_value __cg_x86_64_add(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("variables uninitialized\n");
    return (cg_x86_64_value){.data = NULL};
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("types mismatch\n");
    return (cg_x86_64_value){.data = NULL};
  }

  switch (lsv_data->type.type) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t         l = *lsv_data->data;
    uint8_t         r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(uint8_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_BYTE});
    *data->data = l + r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t         l = *lsv_data->data;
    int32_t         r = *rsv_data->data;
    cg_x86_64_data *data =
        data_new(sizeof(int32_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_INT});
    *data->data = l + r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t        l = *lsv_data->data;
    uint32_t        r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(uint32_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_UINT});
    *data->data = l + r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t         l = *lsv_data->data;
    int64_t         r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(int64_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_LONG});
    *data->data = l + r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t        l = *lsv_data->data;
    uint64_t        r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(uint64_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_ULONG});
    *data->data = l + r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_CHAR: {
    char            l = *lsv_data->data;
    char            r = *rsv_data->data;
    cg_x86_64_data *data =
        data_new(sizeof(char), (cg_x86_64_type){.type = CG_X86_64_TYPE_CHAR});
    *data->data = l + r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_STRING: {
    char           *l = lsv_data->data;
    char           *r = rsv_data->data;
    cg_x86_64_data *data =
        data_new(lsv_data->data_size + rsv_data->data_size - 1,
                 (cg_x86_64_type){.type = CG_X86_64_TYPE_STRING});
    memcpy(data->data, l, lsv_data->data_size - 1);
    memcpy(data->data + lsv_data->data_size - 1, r, rsv_data->data_size - 1);
    data->data[lsv_data->data_size + rsv_data->data_size - 2] = '\0';
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("type implementation undefined\n");
    return (cg_x86_64_value){.data = NULL};
  }
}

cg_x86_64_value __cg_x86_64_sub(cg_x86_64_value *lsv, cg_x86_64_value *rsv) {
  if (!lsv->data || !rsv->data) {
    printf("variables uninitialized\n");
    return (cg_x86_64_value){.data = NULL};
  }

  cg_x86_64_data *lsv_data, *rsv_data;
  lsv_data = lsv->data;
  rsv_data = rsv->data;

  if (!types_equals(&lsv_data->type, &rsv_data->type)) {
    printf("types mismatch\n");
    return (cg_x86_64_value){.data = NULL};
  }

  switch (lsv_data->type.type) {
  case CG_X86_64_TYPE_BYTE: {
    uint8_t         l = *lsv_data->data;
    uint8_t         r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(uint8_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_BYTE});
    *data->data = l - r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_INT: {
    int32_t         l = *lsv_data->data;
    int32_t         r = *rsv_data->data;
    cg_x86_64_data *data =
        data_new(sizeof(int32_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_INT});
    *data->data = l - r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_UINT: {
    uint32_t        l = *lsv_data->data;
    uint32_t        r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(uint32_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_UINT});
    *data->data = l - r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_LONG: {
    int64_t         l = *lsv_data->data;
    int64_t         r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(int64_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_LONG});
    *data->data = l - r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_ULONG: {
    uint64_t        l = *lsv_data->data;
    uint64_t        r = *rsv_data->data;
    cg_x86_64_data *data = data_new(
        sizeof(uint64_t), (cg_x86_64_type){.type = CG_X86_64_TYPE_ULONG});
    *data->data = l - r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_CHAR: {
    char            l = *lsv_data->data;
    char            r = *rsv_data->data;
    cg_x86_64_data *data =
        data_new(sizeof(char), (cg_x86_64_type){.type = CG_X86_64_TYPE_CHAR});
    *data->data = l - r;
    return (cg_x86_64_value){.data = data};
  } break;
  case CG_X86_64_TYPE_STRING:
  case CG_X86_64_TYPE_BOOL:
  case CG_X86_64_TYPE_ARRAY:
  case CG_X86_64_TYPE_VOID:
  default:
    printf("type implementation undefined\n");
    return (cg_x86_64_value){.data = NULL};
  }
}

cg_x86_64_value __cg_x86_64_mul(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_div(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_mod(cg_x86_64_value *lsv, cg_x86_64_value *rsv);

cg_x86_64_value __cg_x86_64_index(cg_x86_64_value *lsv, size_t size, ...);
cg_x86_64_value __cg_x86_64_cast(cg_x86_64_value *lsv, cg_x86_64_type_enum);
