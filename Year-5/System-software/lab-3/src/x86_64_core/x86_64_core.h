#pragma once

#include <stddef.h>
#include <stdint.h>

typedef enum x86_64_type_enum {
  X86_64_TYPE_VOID,
  X86_64_TYPE_BOOL,
  X86_64_TYPE_BYTE,
  X86_64_TYPE_INT,
  X86_64_TYPE_UINT,
  X86_64_TYPE_LONG,
  X86_64_TYPE_ULONG,
  X86_64_TYPE_CHAR,
  X86_64_TYPE_STRING,
  X86_64_TYPE_ARRAY,
} x86_64_type_enum;

// first type_next (that should be null) is used
// to store metadata (cg_x86_64_array_meta) about array dimensions
typedef struct __attribute__((packed)) x86_64_array_meta_struct {
  uint64_t elem_size;
  uint64_t rank;
  uint64_t dimensions[0];
} x86_64_array_meta;

typedef struct x86_64_type_struct x86_64_type;
typedef struct __attribute__((packed)) x86_64_type_struct {
  x86_64_type_enum kind;
  x86_64_type     *next;
  char             pad[4];
} x86_64_type;

typedef struct __attribute__((packed)) x86_64_data_struct {
  x86_64_type type;
  uint64_t    refs;
  uint64_t    data_size;
  union {
    x86_64_array_meta *meta_array;
    void              *meta;
  };
  uint8_t raw[0];
} x86_64_data;

typedef struct __attribute__((packed)) x86_64_value_struct {
  x86_64_data *data;
} x86_64_value;

// UTILITY
size_t __x86_64_type_direct_size(x86_64_type_enum type);

// VALUE
void __x86_64_value_init(x86_64_value *ref);
void __x86_64_value_drop(x86_64_value *ref);

x86_64_value __x86_64_value_void();
x86_64_value __x86_64_value_bool(uint8_t value);
x86_64_value __x86_64_value_byte(uint8_t value);
x86_64_value __x86_64_value_int(int32_t value);
x86_64_value __x86_64_value_uint(uint32_t value);
x86_64_value __x86_64_value_long(int64_t value);
x86_64_value __x86_64_value_ulong(uint64_t value);
x86_64_value __x86_64_value_char(uint8_t value);
x86_64_value __x86_64_value_string(const uint8_t *value);
// ... - array sizes of type == cg_x86_64_value
x86_64_value __x86_64_value_array(x86_64_type *elem_type, uint64_t rank, ...);

// UNARY
x86_64_value __x86_64_plus(x86_64_value lsv);
x86_64_value __x86_64_minus(x86_64_value lsv);
x86_64_value __x86_64_not(x86_64_value lsv);
x86_64_value __x86_64_bit_not(x86_64_value lsv);
x86_64_value __x86_64_inc(x86_64_value lsv);
x86_64_value __x86_64_dec(x86_64_value lsv);

// BINARY
x86_64_value __x86_64_or(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_and(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_bit_or(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_bit_xor(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_bit_and(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_eq(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_neq(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_less(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_less_eq(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_bit_shl(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_bit_shr(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_add(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_sub(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_mul(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_div(x86_64_value lsv, x86_64_value rsv);
x86_64_value __x86_64_mod(x86_64_value lsv, x86_64_value rsv);

// SPECIAL

// ... -> x86_64_value
// in array arguments are passed in reserve order
// so arr[1,2] -> index(arr, 2, 2, 1), x86_64_value
x86_64_value __x86_64_index_get(x86_64_value array, uint64_t size, ...);
void __x86_64_index_set(x86_64_value from, x86_64_value array, uint64_t size,
                        ...);
x86_64_value __x86_64_cast(x86_64_value value, x86_64_type_enum);
void         __x86_64_assign(x86_64_value *lsv, x86_64_value rsv);

uint8_t  __x86_64_unwrap_bool(x86_64_value value);
uint8_t  __x86_64_unwrap_byte(x86_64_value value);
int32_t  __x86_64_unwrap_int(x86_64_value value);
uint32_t __x86_64_unwrap_uint(x86_64_value value);
int64_t  __x86_64_unwrap_long(x86_64_value value);
uint64_t __x86_64_unwrap_ulong(x86_64_value value);
uint8_t  __x86_64_unwrap_char(x86_64_value value);
uint8_t *__x86_64_unwrap_string(x86_64_value value);
uint8_t *__x86_64_unwrap_array(x86_64_value value);
