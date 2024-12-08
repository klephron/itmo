#pragma once

#include "x86_64_core/type.h"

typedef struct __attribute__((packed)) x86_64_data_struct {
  x86_64_type *type;
  uint64_t     refs;
  uint64_t     raw_size;
  uint8_t      raw[0];
} x86_64_data;

typedef struct __attribute__((packed)) x86_64_value_struct {
  x86_64_data *data;
} x86_64_value;

x86_64_data *x86_64_data_new(size_t data_size, x86_64_type *type);

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

// flattened version of array
x86_64_value __x86_64_value_array(const x86_64_type *elem_type, ...);

uint8_t  __x86_64_unwrap_bool(x86_64_value value);
uint8_t  __x86_64_unwrap_byte(x86_64_value value);
int32_t  __x86_64_unwrap_int(x86_64_value value);
uint32_t __x86_64_unwrap_uint(x86_64_value value);
int64_t  __x86_64_unwrap_long(x86_64_value value);
uint64_t __x86_64_unwrap_ulong(x86_64_value value);
uint8_t  __x86_64_unwrap_char(x86_64_value value);
uint8_t *__x86_64_unwrap_string(x86_64_value value);
void    *__x86_64_unwrap_array(x86_64_value value);
