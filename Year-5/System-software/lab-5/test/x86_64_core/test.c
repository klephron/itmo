#include <criterion/criterion.h>
#include <stdint.h>

#include "util/log.h"
#include "x86_64_core/proxy/value/bool.h"
#include "x86_64_core/proxy/value/void.h"
#include "x86_64_core/value.h"

Test(x86_64_bool, test1) {
  x86_64_value value1;
  x86_64_value value2;
  x86_64_value value3;
  x86_64_value value4;

  __x86_64_proxy_void_init(&value1);

}
