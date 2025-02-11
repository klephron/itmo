// #include "ctx_build.h"

// #include "x86_64_core/meta.h"

// #include <string.h>

// void cg_x86_64_ctx_build(cg_x86_64_ctx *ctx, mir *mir) {
//   list_cg_x86_64_unit *globl_units = list_cg_x86_64_unit_new();
//   list_cg_x86_64_unit *ext_units = list_cg_x86_64_unit_new();

//   // core declarations
//   for (int kind = 0; kind < X86_64_SYMBOL_UNKNOWN; ++kind) {
//     list_cg_x86_64_unit_push_back(
//         ext_units,
//         (cg_x86_64_unit *)cg_x86_64_dir_extern_new(cg_x86_64_label_new(
//             strdup(x86_64_symbol_enum_str(kind)),
//             CG_X86_64_LABEL_DIRECTIVE)));
//   }

//   // executable declarations
//   for (list_mir_subroutine_ext_it it =
//            list_mir_subroutine_ext_begin(mir->sub_exts);
//        !it.end(&it); it.next(&it)) {
//     mir_subroutine_ext *ext = it.get(&it);
//     char               *ext_name = strdup(ext->name);

//     list_cg_x86_64_unit_push_back(
//         ext_units,
//         (cg_x86_64_unit *)cg_x86_64_dir_extern_new(
//             cg_x86_64_label_new(ext_name, CG_X86_64_LABEL_DIRECTIVE)));

//     hashset_cg_x86_64_mir_ref_insert(ctx->globls, (mir_node_base *)ext);
//   }

//   // add to globals
//   for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
//        !it.end(&it); it.next(&it)) {
//     mir_subroutine *sub = it.get(&it);

//     if (!strcmp(sub->name, "main")) {
//       list_cg_x86_64_unit_push_back(
//           globl_units,
//           (cg_x86_64_unit *)cg_x86_64_dir_globl_new(cg_x86_64_label_new(
//               strdup(sub->name), CG_X86_64_LABEL_DIRECTIVE)));

//       hashset_cg_x86_64_mir_ref_insert(ctx->globls, (mir_node_base *)sub);
//     }
//   }

//   cg_units_extend_move(ctx->cg_ref->text->units, ext_units);
//   cg_units_extend_move(ctx->cg_ref->text->units, globl_units);

//   for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
//        !it.end(&it); it.next(&it)) {
//     ctx_build_sub(ctx, it.get(&it), ctx->debug_subs_cnt++);
//   }

//   ctx_debug_info_post_push_header(ctx);
//   ctx_debug_info_post_push_files(ctx);
//   ctx_debug_line_post_push_header(ctx);
//   ctx_debug_str_post_push_header(ctx);
// }
