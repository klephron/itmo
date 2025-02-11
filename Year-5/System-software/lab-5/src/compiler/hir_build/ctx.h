// #pragma once

// #include "compiler/ast/ast.h"
// #include "compiler/exception/container.h"
// #include "compiler/hir/hir.h"
// #include "util/hashset.h"
// #include "util/list.h"

// // SCOPE
// typedef enum hir_ctx_scope_enum {
//   HIR_CTX_SCOPE_UNKNOWN,
//   HIR_CTX_SCOPE_GLOBAL,
//   HIR_CTX_SCOPE_SUBROUTINE,
//   HIR_CTX_SCOPE_BLOCK,
//   HIR_CTX_SCOPE_EXPRESSION
// } hir_ctx_scope_enum;

// typedef struct hir_ctx_scope_entry_struct {
//   char       *name_ref;
//   hir_symbol *binding_ref;
// } hir_ctx_scope_entry;

// hir_ctx_scope_entry *hir_ctx_scope_entry_new(char       *id_ref,
//                                              hir_symbol *binding_ref);
// void                 hir_ctx_scope_entry_free(hir_ctx_scope_entry *self);

// static inline int container_cmp_hir_ctx_scope_entry(const void *_lsv,
//                                                     const void *_rsv) {
//   const hir_ctx_scope_entry *lsv = _lsv;
//   const hir_ctx_scope_entry *rsv = _rsv;
//   return container_cmp_chars(lsv->name_ref, rsv->name_ref);
// }
// static inline size_t container_hash_hir_ctx_scope_entry(const void *_lsv) {
//   const hir_ctx_scope_entry *lsv = _lsv;
//   return container_hash_chars(lsv->name_ref);
// }
// static inline void container_delete_hir_ctx_scope_entry(void *_lsv) {
//   hir_ctx_scope_entry_free(_lsv);
// }
// HASHSET_DECLARE_STATIC_INLINE(hashset_hir_ctx_scope_entry,
// hir_ctx_scope_entry,
//                               container_cmp_hir_ctx_scope_entry,
//                               container_new_move, container_delete_ptr,
//                               container_hash_hir_ctx_scope_entry);

// typedef struct hir_ctx_scope_struct {
//   hir_ctx_scope_enum           kind;
//   hashset_hir_ctx_scope_entry *hashset;
// } hir_ctx_scope;

// hir_ctx_scope *hir_ctx_scope_new(hir_ctx_scope_enum kind);
// void           hir_ctx_scope_free(hir_ctx_scope *self);

// static inline void container_delete_hir_ctx_scope(void *_data) {
//   hir_ctx_scope_free(_data);
// }
// LIST_DECLARE_STATIC_INLINE(list_hir_ctx_scope, hir_ctx_scope,
//                            container_cmp_false, container_new_move,
//                            container_delete_hir_ctx_scope);

// // CTX
// typedef struct hir_ctx_struct {
//   hir            *hir_ref;
//   list_exception *exceptions;
//   // internal data
//   const char         *ast_stream_ref;
//   list_hir_ctx_scope *scopes;
// } hir_ctx;

// hir_ctx *hir_ctx_new(hir *hir_ref);
// void     hir_ctx_reset_exceptions(hir_ctx *self);
// void     hir_ctx_free(hir_ctx *self);

// void hir_ctx_build(hir_ctx *ctx, const ast *ast);
// void hir_ctx_bind(hir_ctx *ctx);
// void hir_ctx_type(hir_ctx *ctx);
