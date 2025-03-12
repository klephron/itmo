#pragma once

#include "x86_64/data.h"

// make
x86_64_value __x86_64_make_void();
x86_64_value __x86_64_make_bool();
x86_64_value __x86_64_make_byte();
x86_64_value __x86_64_make_int();
x86_64_value __x86_64_make_uint();
x86_64_value __x86_64_make_long();
x86_64_value __x86_64_make_ulong();
x86_64_value __x86_64_make_char();
x86_64_value __x86_64_make_string();
x86_64_value __x86_64_make_array();
x86_64_value __x86_64_make_object();
x86_64_value __x86_64_make_reference();
x86_64_value __x86_64_make_error();
