#include "x86_64_build.h"

#include "util/macro.h"

cg_x86_64_build_result cg_x86_64_build(const mir *mir) {
  cg_x86_64_build_result result = {
      .code       = cg_x86_64_new(),
      .exceptions = list_exception_new(),
  };

  UNUSED(mir);

  return result;
}
