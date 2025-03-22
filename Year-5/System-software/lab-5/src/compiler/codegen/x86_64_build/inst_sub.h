#pragma once

#include "compiler/codegen/x86_64/x86_64.h"
#include "compiler/exception/list.h"
#include "compiler/mir/mir.h"
#include "util/hashset.h"

typedef struct cg_inst_sub_result_struct {
  list_exception *exceptions;
} cg_inst_sub_result;

cg_inst_sub_result cg_inst_subs(cg_x86_64 *code, const mir *mir);

// ctx
typedef struct cg_value_meta_struct {
  const mir_value *value_ref;
  int64_t          offset; // of stack relative to rbp
  int              is_ptr; // is pointer of actual value
} cg_value_meta;

cg_value_meta *cg_value_meta_new(const mir_value *value_ref, int64_t offset,
                                 int is_ptr);
void           cg_value_meta_free(cg_value_meta *self);

static inline int container_cmp_cg_value_meta(const void *lsv,
                                              const void *rsv) {
  const cg_value_meta *l = lsv;
  const cg_value_meta *r = rsv;
  return container_cmp_ptr(l->value_ref, r->value_ref);
}
static inline uint64_t container_hash_cg_value_meta(const void *lsv) {
  const cg_value_meta *l = lsv;
  return container_hash_ptr(l->value_ref);
}
static inline void container_delete_cg_value_meta(void *data) {
  return cg_value_meta_free(data);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_cg_value_meta, cg_value_meta,
                              container_cmp_cg_value_meta, container_new_move,
                              container_delete_cg_value_meta,
                              container_hash_cg_value_meta);

typedef struct cg_ctx_struct {
  cg_x86_64      *code;
  list_exception *exceptions;

  const mir_subroutine  *sub;
  hashset_cg_value_meta *map_value_meta;
  uint64_t               frame_size;

  const mir_bb *bb;
} cg_ctx;

void cg_ctx_init(cg_ctx *ctx, cg_x86_64 *code, list_exception *exceptions);
void cg_ctx_deinit(cg_ctx *ctx);

cg_value_meta *cg_ctx_value_meta_find(cg_ctx *ctx, const mir_value *value);
cg_value_meta *cg_ctx_value_meta_emplace(cg_ctx          *ctx,
                                         const mir_value *value_ref,
                                         int64_t offset, int is_ptr);

void cg_ctx_text_push_back(cg_ctx *ctx, void *unit);
void cg_ctx_data_push_back(cg_ctx *ctx, void *unit);
void cg_ctx_debug_info_push_back(cg_ctx *ctx, void *unit);
void cg_ctx_debug_line_push_back(cg_ctx *ctx, void *unit);
void cg_ctx_debug_str_push_back(cg_ctx *ctx, void *unit);

uint64_t cg_ctx_text_emplace_back_text(cg_ctx *ctx, cg_x86_64_mnem mnem, ...);

int64_t cg_ctx_rbp_offset(cg_ctx *ctx);

// labels
char *cg_label_sub_global(const mir_subroutine *sub);
char *cg_label_sub_global_suf(const mir_subroutine *sub, const char *suffix);
char *cg_label_sub_global_bb(const mir_subroutine *sub, const mir_bb *bb);

// utility
uint64_t cg_aligned(uint64_t size);

// bb
void cg_inst_sub_bbs(cg_ctx *ctx, const list_mir_bb *bbs);
