// #include "ctx.h"

// #include "util/macro.h"

// cg_x86_64_map_value *cg_x86_64_map_value_new(const mir_value *value_ref,
//                                              size_t           rbp_offset) {
//   cg_x86_64_map_value *self = MALLOC(cg_x86_64_map_value);
//   self->value_ref = value_ref;
//   self->rbp_offset = rbp_offset;
//   return self;
// }

// void cg_x86_64_map_value_free(cg_x86_64_map_value *self) {
//   if (self) {
//     free(self);
//   }
// }

// cg_x86_64_debug_str *cg_x86_64_debug_str_new_file(uint64_t    file_id,
//                                                   uint64_t    str_id,
//                                                   const char *chars_ref) {
//   cg_x86_64_debug_str *self = MALLOC(cg_x86_64_debug_str);
//   self->file_id = file_id;
//   self->str_id = str_id;
//   self->chars_ref = chars_ref;
//   return self;
// }

// void cg_x86_64_debug_str_free(cg_x86_64_debug_str *self) {
//   if (self) {
//     free(self);
//   }
// }

// cg_x86_64_ctx *cg_x86_64_ctx_new(cg_x86_64 *cg_ref) {
//   cg_x86_64_ctx *self = MALLOC(cg_x86_64_ctx);
//   self->cg_ref = cg_ref;
//   self->exceptions = list_exception_new();
//   self->globls = hashset_cg_x86_64_mir_ref_new();
//   self->debug_subs_cnt = 0;
//   self->debug_files_cnt = 0;
//   self->debug_strs_cnt = 0;
//   self->debug_lines_cnt = 0;
//   self->debug_files_idx = hashset_cg_x86_64_debug_str_new();
//   self->debug_strs = list_cg_x86_64_chars_new();
//   return self;
// }

// void cg_x86_64_ctx_free(cg_x86_64_ctx *self) {
//   if (self) {
//     self->cg_ref = NULL;
//     list_exception_free(self->exceptions);
//     hashset_cg_x86_64_mir_ref_free(self->globls);
//     hashset_cg_x86_64_debug_str_free(self->debug_files_idx);
//     list_cg_x86_64_chars_free(self->debug_strs);
//     free(self);
//   }
// }
