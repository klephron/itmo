#include "ctx_bb_merge.h"
#include "util/hashset.h"
#include "util/list.h"
#include "util/macro.h"

LIST_DECLARE_STATIC_INLINE(list_mir_bb_ref, mir_bb, container_cmp_false,
                           container_new_move, container_delete_false);

typedef struct mir_bb_preds_struct {
  const mir_bb    *bb;
  list_mir_bb_ref *preds;
} mir_bb_preds;

mir_bb_preds *mir_bb_preds_new(const mir_bb *bb) {
  mir_bb_preds *self = MALLOC(mir_bb_preds);
  self->bb = bb;
  self->preds = list_mir_bb_ref_new();
  return self;
}

static inline int container_cmp_mir_bb_preds(const void *_lsv,
                                             const void *_rsv) {
  const mir_bb_preds *lsv = _lsv;
  const mir_bb_preds *rsv = _rsv;
  return container_cmp_ptr(lsv->bb, rsv->bb);
}
static inline void container_delete_mir_bb_preds(void *data) {
  mir_bb_preds *self = data;
  list_mir_bb_ref_free(self->preds);
  free(self);
}
static inline size_t container_hash_mir_bb_preds(const void *data) {
  const mir_bb_preds *self = data;
  return container_hash_ptr(self->bb);
}

HASHSET_DECLARE_STATIC_INLINE(hashset_mir_bb_preds, mir_bb_preds,
                              container_cmp_mir_bb_preds, container_new_move,
                              container_delete_mir_bb_preds,
                              container_hash_mir_bb_preds);

HASHSET_DECLARE_STATIC_INLINE(hashset_mir_bb, mir_bb, container_cmp_ptr,
                              container_new_move, container_delete_false,
                              container_hash_ptr);

static mir_bb_preds *mir_ctx_bb_get_preds(mir_ctx              *ctx,
                                          hashset_mir_bb_preds *hs_preds,
                                          hashset_mir_bb       *hs_visited,
                                          const mir_bb         *bb) {
  if (!bb) {
    return NULL;
  }

  // if already traversed than return its predecessor list
  hashset_mir_bb_it bb_it = hashset_mir_bb_find(hs_visited, (mir_bb *)bb);
  if (!bb_it.end(&bb_it)) {
    hashset_mir_bb_preds_it it =
        hashset_mir_bb_preds_find(hs_preds, &(mir_bb_preds){.bb = bb});
    return it.get(&it);
  }
  hashset_mir_bb_insert(hs_visited, (mir_bb *)bb);

  mir_bb_preds *bb_preds = mir_bb_preds_new(bb);
  hashset_mir_bb_preds_insert(hs_preds, bb_preds);

  if (bb->jmp) {
    mir_bb_preds *je_preds =
        mir_ctx_bb_get_preds(ctx, hs_preds, hs_visited, bb->jmp->je_ref);
    if (je_preds) {
      list_mir_bb_ref_push_back(je_preds->preds, (mir_bb *)bb);
    }

    mir_bb_preds *jz_preds =
        mir_ctx_bb_get_preds(ctx, hs_preds, hs_visited, bb->jmp->jz_ref);
    if (jz_preds) {
      list_mir_bb_ref_push_back(jz_preds->preds, (mir_bb *)bb);
    }
  }

  return bb_preds;
}

// returns next block that can be merged
static mir_bb *mir_ctx_bb_can_merge_next(mir_bb *b) {
  if (!b->jmp) { // if last
    return NULL;
  }
  if (b->jmp->je_ref) { // if conditional
    return NULL;
  }
  return b->jmp->jz_ref;
}

mir_bb *mir_ctx_bb_merge_block(mir_ctx *ctx, hashset_mir_bb_preds *hs_preds,
                               hashset_mir_bb *hs_deleted, mir_bb *bb) {
  UNUSED(ctx);

  if (!bb) {
    return NULL;
  }

  // debug("merging... %zu", bb->id);

  mir_bb *next_bb = mir_ctx_bb_can_merge_next(bb);
  if (!next_bb) {
    return NULL;
  }

  hashset_mir_bb_preds_it preds_it =
      hashset_mir_bb_preds_find(hs_preds, &(mir_bb_preds){.bb = bb});
  mir_bb_preds *bb_preds = preds_it.get(&preds_it);

  for (list_mir_bb_ref_it it = list_mir_bb_ref_begin(bb_preds->preds);
       !it.end(&it); it.next(&it)) {
    mir_bb *pred_bb = it.get(&it);

    // check if not already deleted
    hashset_mir_bb_it it = hashset_mir_bb_find(hs_deleted, pred_bb);
    if (!it.end(&it)) {
      continue;
    }

    hashset_mir_bb_preds_it next_it =
        hashset_mir_bb_preds_find(hs_preds, &(mir_bb_preds){.bb = next_bb});
    mir_bb_preds *next_preds = next_it.get(&next_it);

    if (pred_bb->jmp->je_ref == bb) {
      pred_bb->jmp->je_ref = next_bb;
      // debug("upd.. %zu.je_ref -> %zu", pred_bb->id, next_bb->id);
      list_mir_bb_ref_push_back(next_preds->preds, pred_bb);
    }
    if (pred_bb->jmp->jz_ref == bb) {
      pred_bb->jmp->jz_ref = next_bb;
      // debug("upd.. %zu.jz_ref -> %zu", pred_bb->id, next_bb->id);
      list_mir_bb_ref_push_back(next_preds->preds, pred_bb);
    }
  }

  // merge stmt_list to another block
  list_mir_node *rev_stmt = list_mir_node_new();

  while (!list_mir_node_empty(bb->stmts)) {
    mir_node_base *stmt = list_mir_node_pop_front(bb->stmts);
    list_mir_node_push_front(rev_stmt, stmt);
  }

  while (!list_mir_node_empty(rev_stmt)) {
    mir_node_base *stmt = list_mir_node_pop_front(rev_stmt);
    list_mir_node_push_front(next_bb->stmts, stmt);
  }

  list_mir_node_free(rev_stmt);

  return next_bb;
}

void mir_ctx_bb_merge_subroutine(mir_ctx *ctx, mir_subroutine *sub) {
  if (!list_mir_bb_size(sub->bbs)) {
    return;
  }

  hashset_mir_bb_preds *hs_preds = hashset_mir_bb_preds_new();
  hashset_mir_bb       *hs_visited = hashset_mir_bb_new();
  hashset_mir_bb       *hs_deleted = hashset_mir_bb_new();

  mir_bb *first = list_mir_bb_front(sub->bbs);
  mir_ctx_bb_get_preds(ctx, hs_preds, hs_visited, first);

  // check
  // for (hashset_mir_bb_preds_it it = hashset_mir_bb_preds_begin(hs_preds);
  //      !it.end(&it); it.next(&it)) {
  //   mir_bb_preds *bb_preds = it.get(&it);

  //   debug("bb%zu", bb_preds->bb->id);
  //   for (list_mir_bb_ref_it pred_it = list_mir_bb_ref_begin(bb_preds->preds);
  //        !pred_it.end(&pred_it); pred_it.next(&pred_it)) {
  //     debug("  bb%zu", pred_it.get(&pred_it)->id);
  //   }
  // }

  list_mir_bb *new_bbs = list_mir_bb_new();

  // NOTE: bb_first may be redundant (need to check)
  while (!list_mir_bb_empty(sub->bbs)) {
    mir_bb *bb = list_mir_bb_pop_front(sub->bbs);
    mir_bb *next_bb = mir_ctx_bb_merge_block(ctx, hs_preds, hs_deleted, bb);
    if (next_bb) {
      hashset_mir_bb_insert(hs_deleted, bb);
      mir_bb_free(bb);
      if (bb != next_bb && bb == sub->bb_first_ref) {
        sub->bb_first_ref = (mir_bb *)next_bb;
      }
    } else {
      list_mir_bb_push_back(new_bbs, bb);
    }
  }

  list_mir_bb_free(sub->bbs);
  sub->bbs = new_bbs;

  hashset_mir_bb_free(hs_deleted);
  hashset_mir_bb_free(hs_visited);
  hashset_mir_bb_preds_free(hs_preds);
}

void mir_ctx_bb_merge_all(mir_ctx *ctx) {
  for (list_mir_subroutine_it it = list_mir_subroutine_begin(ctx->mir->subs);
       !it.end(&it); it.next(&it)) {
    mir_ctx_bb_merge_subroutine(ctx, it.get(&it));
  }
}
