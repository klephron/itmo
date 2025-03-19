#include "x86_64_build.h"

#include "compiler/codegen/x86_64_build/inst_dir.h"
#include "compiler/codegen/x86_64_build/inst_sub.h"

// static inline void cg_ctx_data_push_back(cg_ctx *ctx, void *unit) {
//   list_cg_x86_64_unit_push_back(ctx->code->data, (cg_x86_64_unit *)unit);
// }

// static inline void cg_ctx_debug_info_push_back(cg_ctx *ctx, void *unit) {
//   list_cg_x86_64_unit_push_back(ctx->code->debug_info, (cg_x86_64_unit
//   *)unit);
// }

// static inline void cg_ctx_debug_line_push_back(cg_ctx *ctx, void *unit) {
//   list_cg_x86_64_unit_push_back(ctx->code->debug_line, (cg_x86_64_unit
//   *)unit);
// }

// static inline void cg_ctx_debug_str_push_back(cg_ctx *ctx, void *unit) {
//   list_cg_x86_64_unit_push_back(ctx->code->debug_str, (cg_x86_64_unit
//   *)unit);
// }

static inline int cg_ok(list_exception *exceptions, int ignore_errors) {
  return exceptions && (list_exception_empty(exceptions) || ignore_errors);
}

cg_x86_64_build_result cg_x86_64_build(const mir *mir, int ignore_errors) {
  cg_x86_64_build_result result = {
      .code       = cg_x86_64_new(),
      .exceptions = list_exception_new(),
  };

  if (cg_ok(result.exceptions, ignore_errors)) {
    cg_inst_dir_result r = cg_inst_dir(result.code, mir);
    list_exception_extend(result.exceptions, r.exceptions);
  }

  if (cg_ok(result.exceptions, ignore_errors)) {
    cg_inst_sub_result r = cg_inst_sub(result.code, mir);
    list_exception_extend(result.exceptions, r.exceptions);
  }

  return result;
}
