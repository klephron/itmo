#pragma once

#include "compiler/codegen/x86_64/x86_64.h"
#include "compiler/exception/list.h"
#include "compiler/mir/mir.h"
#include "util/hashset.h"

typedef struct cg_inst_result_struct {
  list_exception *exceptions;
} cg_inst_result;

cg_inst_result cg_inst(cg_x86_64 *code, const mir *mir);

int cg_inst_sub_extern_check_param(const type_base *param);
int cg_inst_sub_extern_check_ret(const type_base *ret);

// value_meta
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

// mir_sym (to find symbols by mir pointerc)
typedef struct cg_mir_sym_struct {
  union {
    const void           *mir_ref;
    const mir_lit        *lit_ref;
    const mir_subroutine *sub_ref;
  };

  const char *sym_ref;
} cg_mir_sym;

cg_mir_sym *cg_mir_sym_new(const void *mir_ref, const char *sym_ref);
void        cg_mir_sym_free(cg_mir_sym *self);

static inline int container_cmp_cg_mir_sym(const void *lsv, const void *rsv) {
  const cg_mir_sym *l = lsv;
  const cg_mir_sym *r = rsv;
  return container_cmp_ptr(l->mir_ref, r->mir_ref);
}
static inline uint64_t container_hash_cg_mir_sym(const void *lsv) {
  const cg_mir_sym *l = lsv;
  return container_hash_ptr(l->mir_ref);
}
static inline void container_delete_cg_mir_sym(void *data) {
  return cg_mir_sym_free(data);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_cg_mir_sym, cg_mir_sym,
                              container_cmp_cg_mir_sym, container_new_move,
                              container_delete_cg_mir_sym,
                              container_hash_cg_mir_sym);

// ctx
typedef struct cg_ctx_struct {
  hashset_cg_mir_sym *map_mir_sym;
  uint64_t            lit_cnt;

  const mir_subroutine  *sub;
  const char            *sub_sym;
  hashset_cg_value_meta *map_value_meta;
  uint64_t               frame_size;

  const mir_bb *bb;

  cg_x86_64      *code;
  list_exception *exceptions;
} cg_ctx;

void cg_ctx_init(cg_ctx *ctx, cg_x86_64 *code, list_exception *exceptions);
void cg_ctx_deinit(cg_ctx *ctx);

// returns literal symbol name
const char *cg_ctx_mir_sym_find_lit(cg_ctx *ctx, const mir_lit *lit_ref);
char       *cg_ctx_mir_sym_emplace_lit(cg_ctx *ctx, const mir_lit *lit_ref);

const char *cg_ctx_mir_sym_find_sub(cg_ctx *ctx, const mir_subroutine *sub_ref);
void cg_ctx_mir_sym_emplace_sub(cg_ctx *ctx, const mir_subroutine *sub_ref,
                                const char *sym_ref);

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
char *cg_label_local_suf(const char *base, const char *suffix);
char *cg_label_local_bb(const char *base, const mir_bb *bb);

// utility
uint64_t cg_aligned(uint64_t size);

// bb
void cg_inst_bbs(cg_ctx *ctx, const list_mir_bb *bbs);

// core
void cg_inst_core(cg_ctx *ctx);
