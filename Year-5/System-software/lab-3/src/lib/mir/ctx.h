#pragma once

#include "eir/eir.h"
#include "exception/container.h"
#include "mir/mir.h"
#include "util/hashset.h"

// builds MIR from EIR (which is useless, exists only for lab purpose)

// MAPPINGS
typedef struct mir_map_sub_struct {
  const hir_symbol    *hir;
  const mir_node_base *mir;
} mir_map_sub;

mir_map_sub *mir_map_sub_new(const hir_symbol *hir, const mir_node_base *mir);
void         mir_map_sub_free(mir_map_sub *self);

static inline int container_cmp_mir_map_sub(const void *_lsv,
                                            const void *_rsv) {
  const mir_map_sub *lsv = _lsv;
  const mir_map_sub *rsv = _rsv;
  return lsv->hir != rsv->hir;
}
static inline void container_delete_mir_map_sub(void *rsv) {
  mir_map_sub_free(rsv);
}
static inline size_t container_hash_mir_map_sub(const void *rsv) {
  const mir_map_sub *lsv = rsv;
  return container_hash_ptr(lsv->hir);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_mir_map_sub, mir_map_sub,
                              container_cmp_mir_map_sub, container_new_move,
                              container_delete_mir_map_sub,
                              container_hash_mir_map_sub);

typedef struct mir_map_value_struct {
  const hir_symbol *hir;
  const mir_value  *mir;
} mir_map_value;

mir_map_value *mir_map_value_new(const hir_symbol *hir, const mir_value *mir);
void           mir_map_value_free(mir_map_value *self);

static inline int container_cmp_mir_map_value(const void *_lsv,
                                              const void *_rsv) {
  const mir_map_value *lsv = _lsv;
  const mir_map_value *rsv = _rsv;
  return lsv->hir != rsv->hir;
}
static inline void container_delete_mir_map_value(void *rsv) {
  mir_map_value_free(rsv);
}
static inline size_t container_hash_mir_map_value(const void *rsv) {
  const mir_map_sub *lsv = rsv;
  return container_hash_ptr(lsv->hir);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_mir_map_value, mir_map_value,
                              container_cmp_mir_map_value, container_new_move,
                              container_delete_mir_map_value,
                              container_hash_mir_map_value);

typedef struct mir_map_bb_struct {
  const eir_block_base *hir;
  const mir_bb         *mir;
} mir_map_bb;

mir_map_bb *mir_map_bb_new(const eir_block_base *hir, const mir_bb *mir);
void        mir_map_bb_free(mir_map_bb *self);

static inline int container_cmp_mir_map_bb(const void *_lsv, const void *_rsv) {
  const mir_map_bb *lsv = _lsv;
  const mir_map_bb *rsv = _rsv;
  return lsv->hir != rsv->hir;
}
static inline void container_delete_mir_map_bb(void *rsv) {
  mir_map_bb_free(rsv);
}
static inline size_t container_hash_mir_map_bb(const void *rsv) {
  const mir_map_sub *lsv = rsv;
  return container_hash_ptr(lsv->hir);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_mir_map_bb, mir_map_bb,
                              container_cmp_mir_map_bb, container_new_move,
                              container_delete_mir_map_bb,
                              container_hash_mir_map_bb);

// CTX
typedef struct mir_ctx_struct {
  mir            *mir;
  list_exception *exceptions;
  // internal
  size_t                 value_cnt;
  hashset_mir_map_sub   *map_subs;
  hashset_mir_map_value *map_lvalues;
  hashset_mir_map_bb    *map_bbs;
  mir_node_base         *stmt_last; // topological last
  mir_bb                *bb_last;
} mir_ctx;

mir_ctx *mir_ctx_new(mir *mir);
void     mir_ctx_free(mir_ctx *self);

void mir_ctx_build(mir_ctx *self, const eir *eir);
void mir_ctx_bb_merge(mir_ctx *self);
