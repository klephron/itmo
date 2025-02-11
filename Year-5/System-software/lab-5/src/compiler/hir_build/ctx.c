// #include "ctx.h"

// #include "compiler/antlr3/tree/HirBuilder.h"
// #include "compiler/exception/container.h"
// #include "util/macro.h"

// // SCOPE
// hir_ctx_scope_entry *hir_ctx_scope_entry_new(char       *name_ref,
//                                              hir_symbol *binding_ref) {
//   hir_ctx_scope_entry *self = MALLOC(hir_ctx_scope_entry);
//   self->name_ref = name_ref;
//   self->binding_ref = binding_ref;
//   return self;
// }

// void hir_ctx_scope_entry_free(hir_ctx_scope_entry *self) {
//   if (self) {
//     free(self);
//   }
// }

// hir_ctx_scope *hir_ctx_scope_new(hir_ctx_scope_enum kind) {
//   hir_ctx_scope *self = MALLOC(hir_ctx_scope);
//   self->kind = kind;
//   self->hashset = hashset_hir_ctx_scope_entry_new();
//   return self;
// }

// void hir_ctx_scope_free(hir_ctx_scope *self) {
//   if (self) {
//     hashset_hir_ctx_scope_entry_free(self->hashset);
//     free(self);
//   }
// }

// // CTX
// hir_ctx *hir_ctx_new(hir *hir_ref) {
//   hir_ctx *self = MALLOC(hir_ctx);
//   self->hir_ref = hir_ref;
//   self->exceptions = list_exception_new();
//   self->ast_stream_ref = NULL;
//   self->scopes = NULL;
//   return self;
// }

// void hir_ctx_reset_exceptions(hir_ctx *self) {
//   list_exception_free(self->exceptions);
//   self->exceptions = list_exception_new();
// }

// void hir_ctx_free(hir_ctx *self) {
//   if (self) {
//     self->hir_ref = NULL;
//     list_exception_free(self->exceptions);
//     self->exceptions = NULL;
//     self->ast_stream_ref = NULL;
//     self->scopes = NULL;
//     free(self);
//   }
// }
