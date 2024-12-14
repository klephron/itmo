#pragma once

#include "compiler/codegen/x86_64.h"
#include "compiler/exception/container.h"
#include "compiler/mir/mir.h"
#include "util/hashset.h"

typedef struct cg_x86_64_map_value_struct {
  const mir_value *value_ref;
  size_t           rbp_offset;
} cg_x86_64_map_value;

cg_x86_64_map_value *cg_x86_64_map_value_new(const mir_value *value_ref,
                                             size_t           rbp_offset);
void                 cg_x86_64_map_value_free(cg_x86_64_map_value *self);

static inline int container_cmp_cg_x86_64_map_value(const void *_lsv,
                                                    const void *_rsv) {
  const cg_x86_64_map_value *lsv = _lsv;
  const cg_x86_64_map_value *rsv = _rsv;
  return lsv->value_ref != rsv->value_ref;
}
static inline void container_delete_cg_x86_64_map_value(void *rsv) {
  cg_x86_64_map_value_free(rsv);
}
static inline size_t container_hash_cg_x86_64_map_value(const void *rsv) {
  const cg_x86_64_map_value *lsv = rsv;
  return container_hash_ptr(lsv->value_ref);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_cg_x86_64_map_value, cg_x86_64_map_value,
                              container_cmp_cg_x86_64_map_value,
                              container_new_move,
                              container_delete_cg_x86_64_map_value,
                              container_hash_cg_x86_64_map_value);

LIST_DECLARE_STATIC_INLINE(list_cg_x86_64_map_value, cg_x86_64_map_value,
                           container_cmp_cg_x86_64_map_value,
                           container_new_move,
                           container_delete_cg_x86_64_map_value);

HASHSET_DECLARE_STATIC_INLINE(hashset_cg_x86_64_mir_ref, mir_node_base,
                              container_cmp_ptr, container_new_move,
                              container_delete_false, container_hash_ptr);

typedef struct cg_x86_64_ctx_struct {
  cg_x86_64      *cg_ref;
  list_exception *exceptions;

  // internal
  hashset_cg_x86_64_map_value *sub_map_values; // sub var metainfo
  size_t                       sub_stack_size; // offset of rsp from sub call
  size_t                       sub_rbp_offset;

  hashset_cg_x86_64_mir_ref *globls; // ref mir structs to handle global labels
} cg_x86_64_ctx;

cg_x86_64_ctx *cg_x86_64_ctx_new(cg_x86_64 *cg_ref);
void           cg_x86_64_ctx_free(cg_x86_64_ctx *self);
void           cg_x86_64_ctx_build(cg_x86_64_ctx *self, mir *mir);
char          *cg_x86_64_ctx_emit(cg_x86_64_ctx *self);
