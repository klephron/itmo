#pragma once

#include "x86_64/value.h"
#include "x86_64/value/object.h"

// make
extern void (*__x86_64_make_void)(x86_64_value *out);
extern void (*__x86_64_make_bool)(x86_64_value *out, uint8_t value);
extern void (*__x86_64_make_byte)(x86_64_value *out, uint8_t value);
extern void (*__x86_64_make_int)(x86_64_value *out, int32_t value);
extern void (*__x86_64_make_uint)(x86_64_value *out, uint32_t value);
extern void (*__x86_64_make_long)(x86_64_value *out, int64_t value);
extern void (*__x86_64_make_ulong)(x86_64_value *out, uint64_t value);
extern void (*__x86_64_make_char)(x86_64_value *out, uint8_t value);
extern void (*__x86_64_make_string)(x86_64_value *out, const uint8_t *value);
extern void (*__x86_64_make_array)(x86_64_value *out, uint64_t size);
extern void (*__x86_64_make_object)(x86_64_value                     *out,
                                    const x86_64_data_object_symbols *symbols);
extern void (*__x86_64_make_error)(x86_64_value *out, x86_64_value *value);
