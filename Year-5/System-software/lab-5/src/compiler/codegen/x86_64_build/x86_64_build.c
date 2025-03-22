#include "x86_64_build.h"

#include "compiler/codegen/x86_64_build/inst.h"

static inline int cg_ok(list_exception *exceptions, int ignore_errors) {
  return exceptions && (list_exception_empty(exceptions) || ignore_errors);
}

cg_x86_64_build_result cg_x86_64_build(const mir *mir, int ignore_errors) {
  cg_x86_64_build_result result = {
      .code       = cg_x86_64_new(),
      .exceptions = list_exception_new(),
  };

  if (cg_ok(result.exceptions, ignore_errors)) {
    cg_inst_result r = cg_inst(result.code, mir);
    list_exception_extend(result.exceptions, r.exceptions);
  }

  return result;
}
