#include "builtin.h"

#include "x86_64/proxy/data.h"
#include "x86_64/value.h"

x86_64_value __x86_64_make_void() {
  return (x86_64_value){
      .data = (x86_64_data *)__x86_64_proxy_void_new(),
  };
}

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
