#include "data.h"

#include "x86_64/builtin/builtin.h"
#include "x86_64/proxy/registry.h"
#include "x86_64/value.h"

x86_64_data_void *__x86_64_proxy_void_new() {
  return (x86_64_data_void *)&X86_64_REGISTRY.data_void;
}

void __x86_64_proxy_void_op_drop(x86_64_value *self) {
  *self = __x86_64_make_void();
}
