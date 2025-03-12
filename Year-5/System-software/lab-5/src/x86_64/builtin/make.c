#include "builtin.h"
#include "x86_64/proxy/data/array.h"
#include "x86_64/proxy/data/bool.h"
#include "x86_64/proxy/data/byte.h"
#include "x86_64/proxy/data/char.h"
#include "x86_64/proxy/data/error.h"
#include "x86_64/proxy/data/int.h"
#include "x86_64/proxy/data/long.h"
#include "x86_64/proxy/data/object.h"
#include "x86_64/proxy/data/ref.h"
#include "x86_64/proxy/data/string.h"
#include "x86_64/proxy/data/uint.h"
#include "x86_64/proxy/data/ulong.h"
#include "x86_64/proxy/data/void.h"

void (*__x86_64_make_void)(x86_64_value *out) = __x86_64_proxy_void_init;

void (*__x86_64_make_bool)(x86_64_value *out,
                           uint8_t       value) = __x86_64_proxy_bool_init;

void (*__x86_64_make_byte)(x86_64_value *out,
                           uint8_t       value) = __x86_64_proxy_byte_init;

void (*__x86_64_make_int)(x86_64_value *out,
                          int32_t       value) = __x86_64_proxy_int_init;

void (*__x86_64_make_uint)(x86_64_value *out,
                           uint32_t      value) = __x86_64_proxy_uint_init;

void (*__x86_64_make_long)(x86_64_value *out,
                           int64_t       value) = __x86_64_proxy_long_init;

void (*__x86_64_make_ulong)(x86_64_value *out,
                            uint64_t      value) = __x86_64_proxy_ulong_init;

void (*__x86_64_make_char)(x86_64_value *out,
                           uint8_t       value) = __x86_64_proxy_char_init;

void (*__x86_64_make_string)(x86_64_value  *out,
                             const uint8_t *value) = __x86_64_proxy_string_init;

void (*__x86_64_make_array)(x86_64_value *out,
                            uint64_t      size) = __x86_64_proxy_array_init;

void (*__x86_64_make_object)(x86_64_value                     *out,
                             const x86_64_data_object_symbols *symbols) =
    __x86_64_proxy_object_init;

void (*__x86_64_make_ref)(x86_64_value *out,
                          x86_64_value *value) = __x86_64_proxy_ref_init;

void (*__x86_64_make_error)(x86_64_value *out,
                            x86_64_value *value) = __x86_64_proxy_error_init;
