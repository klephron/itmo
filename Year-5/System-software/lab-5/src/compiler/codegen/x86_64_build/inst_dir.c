#include "inst_dir.h"
#include "util/macro.h"
#include <string.h>

typedef struct cg_ctx_struct {
  cg_x86_64      *code;
  list_exception *exceptions;
} cg_ctx;

static void cg_ctx_init(cg_ctx *ctx, cg_x86_64 *code,
                        list_exception *exceptions) {
  ctx->code       = code;
  ctx->exceptions = exceptions;
}

static void cg_ctx_deinit(cg_ctx *ctx) {
  ctx->code       = NULL;
  ctx->exceptions = NULL;
}

static inline void cg_ctx_text_push_back(cg_ctx *ctx, void *unit) {
  list_cg_x86_64_unit_push_back(ctx->code->text, (cg_x86_64_unit *)unit);
}

static void cg_add_dir_extern_core(cg_ctx *ctx, const mir *mir) {
  UNUSED(mir);
  // io
  cg_ctx_text_push_back(ctx,
                        cg_x86_64_symbol_new_extern(strdup("__x86_64_flush")));
  // make
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_void")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_bool")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_byte")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_int")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_uint")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_long")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_ulong")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_char")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_string")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_callable")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_array")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_object")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_object_setup")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_make_error")));
  // print
  cg_ctx_text_push_back(ctx,
                        cg_x86_64_symbol_new_extern(strdup("__x86_64_print")));
  // unwrap
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_void")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_bool")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_byte")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_int")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_uint")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_long")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_ulong")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_char")));
  cg_ctx_text_push_back(
      ctx, cg_x86_64_symbol_new_extern(strdup("__x86_64_unwrap_string")));
}

static void cg_add_dir_extern_declared(cg_ctx *ctx, const mir *mir) {
  for (list_mir_subroutine_it it =
           list_mir_subroutine_begin(mir->declared_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *sub = GET(it);
    cg_ctx_text_push_back(
        ctx, cg_x86_64_symbol_new_extern(strdup(sub->symbol_ref->name)));
  }
}

static void cg_add_dir_global_defined(cg_ctx *ctx, const mir *mir) {
  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->defined_subs);
       !END(it); NEXT(it)) {
    const mir_subroutine *sub = GET(it);
    cg_ctx_text_push_back(
        ctx, cg_x86_64_symbol_new_global(strdup(sub->symbol_ref->name)));
  }
}

cg_inst_dir_result cg_inst_dirs(cg_x86_64 *code, const mir *mir) {
  cg_inst_dir_result result = {
      .exceptions = list_exception_new(),
  };

  cg_ctx ctx;
  cg_ctx_init(&ctx, code, result.exceptions);

  cg_add_dir_extern_core(&ctx, mir);
  cg_add_dir_extern_declared(&ctx, mir);
  cg_add_dir_global_defined(&ctx, mir);

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->defined_subs);
       !END(it); NEXT(it)) {
  }

  cg_ctx_deinit(&ctx);

  return result;
}
