#pragma once

#include "x86_64/data/array.h"
#include "x86_64/data/bool.h"
#include "x86_64/data/byte.h"
#include "x86_64/data/char.h"
#include "x86_64/data/error.h"
#include "x86_64/data/int.h"
#include "x86_64/data/long.h"
#include "x86_64/data/object.h"
#include "x86_64/data/ref.h"
#include "x86_64/data/string.h"
#include "x86_64/data/uint.h"
#include "x86_64/data/ulong.h"
#include "x86_64/data/void.h"

// void
x86_64_data_void *__x86_64_proxy_void_new();
x86_64_op_drop    __x86_64_proxy_void_op_drop;

// bool
x86_64_data_bool *__x86_64_proxy_bool_new(uint8_t value);

// byte
x86_64_data_byte *__x86_64_proxy_byte_new(uint8_t value);

// int
x86_64_data_int *__x86_64_proxy_int_new(int32_t value);

// uint
x86_64_data_uint *__x86_64_proxy_uint_new(uint32_t value);

// long
x86_64_data_long *__x86_64_proxy_long_new(int64_t value);

// ulong
x86_64_data_ulong *__x86_64_proxy_ulong_new(uint64_t value);

// char
x86_64_data_char *__x86_64_proxy_char_new(uint8_t value);

// string
x86_64_data_string *__x86_64_proxy_string_new(const uint8_t *value);

// array
x86_64_data_array *__x86_64_proxy_array_new(uint64_t size);

// object
x86_64_data_object *
__x86_64_proxy_object_new(const x86_64_data_object_entries *entries);

// reference
x86_64_data_ref *__x86_64_proxy_ref_new(x86_64_value *value);

// error
x86_64_data_error *__x86_64_proxy_error_new(x86_64_value value);
