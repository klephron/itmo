#pragma once

#include <stddef.h>
#include <stdint.h>

typedef enum cg_x86_64_type_enum {
  CG_X86_64_TYPE_VOID,
  CG_X86_64_TYPE_BOOL,
  CG_X86_64_TYPE_BYTE,
  CG_X86_64_TYPE_INT,
  CG_X86_64_TYPE_UINT,
  CG_X86_64_TYPE_LONG,
  CG_X86_64_TYPE_ULONG,
  CG_X86_64_TYPE_CHAR,
  CG_X86_64_TYPE_STRING,
  CG_X86_64_TYPE_ARRAY,
} cg_x86_64_type_enum;

// first type_next (that should be null) is used
// to store metadata (cg_x86_64_array_meta) about array dimensions
typedef struct __attribute__((packed)) cg_x86_64_array_meta_struct {
  uint64_t elem_size;
  uint64_t rank;
  uint64_t dimensions[0];
} cg_x86_64_array_meta;

typedef struct cg_x86_64_type_struct cg_x86_64_type;
typedef struct __attribute__((packed)) cg_x86_64_type_struct {
  cg_x86_64_type_enum kind;
  cg_x86_64_type     *next;
  char                pad[4];
} cg_x86_64_type;

typedef struct __attribute__((packed)) cg_x86_64_data_struct {
  cg_x86_64_type type;
  uint64_t       refs;
  uint64_t       data_size;
  union {
    cg_x86_64_array_meta *meta_array;
    void                 *meta;
  };
  uint8_t raw[0];
} cg_x86_64_data;

typedef struct __attribute__((packed)) cg_x86_64_value_struct {
  cg_x86_64_data *data;
} cg_x86_64_value;

void __cg_x86_64_init(cg_x86_64_value *ref);
void __cg_x86_64_drop(cg_x86_64_value *ref);

// VALUE
cg_x86_64_value __cg_x86_64_value_void();
cg_x86_64_value __cg_x86_64_value_bool(uint8_t value);
cg_x86_64_value __cg_x86_64_value_byte(uint8_t value);
cg_x86_64_value __cg_x86_64_value_int(int32_t value);
cg_x86_64_value __cg_x86_64_value_uint(uint32_t value);
cg_x86_64_value __cg_x86_64_value_long(int64_t value);
cg_x86_64_value __cg_x86_64_value_ulong(uint64_t value);
cg_x86_64_value __cg_x86_64_value_char(uint8_t value);
cg_x86_64_value __cg_x86_64_value_string(const uint8_t *value);
// ... - array sizes of type == cg_x86_64_value *
cg_x86_64_value __cg_x86_64_value_array(cg_x86_64_type *elem_type,
                                        uint64_t        rank, ...);

// UNARY
cg_x86_64_value __cg_x86_64_plus(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_minus(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_not(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_bit_not(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_inc(cg_x86_64_value *lsv);
cg_x86_64_value __cg_x86_64_dec(cg_x86_64_value *lsv);

// BINARY
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
cg_x86_64_value __cg_x86_64_add(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_sub(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_mul(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_div(cg_x86_64_value *lsv, cg_x86_64_value *rsv);
cg_x86_64_value __cg_x86_64_mod(cg_x86_64_value *lsv, cg_x86_64_value *rsv);

// SPECIAL

// in array arguments are passed in reserve order
// so arr[1,2] -> index(arr, 2, 2, 1)
cg_x86_64_value __cg_x86_64_index(cg_x86_64_value *array, uint64_t size, ...);
void __cg_x86_64_index_set(cg_x86_64_value *from, cg_x86_64_value *array,
                           uint64_t size, ...);
cg_x86_64_value __cg_x86_64_cast(cg_x86_64_value *value, cg_x86_64_type_enum);
cg_x86_64_value __cg_x86_64_assign(cg_x86_64_value *value);
