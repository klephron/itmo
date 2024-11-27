#include "compiler/eir/eir.h"
#include "compiler/dot/dot.h"
#include "compiler/eir/str.h"
#include "compiler/hir/str.h"
#include "compiler/type/str.h"
#include "util/container_util.h"
#include "util/hashset.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"
#include "util/string_buffer.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// EXPR
typedef struct dot_expr_edge_struct {
  size_t id_from;
  size_t id_to;
} dot_expr_edge;

static void dot_expr_edge_free(dot_expr_edge *self);

static inline void container_delete_expr_edge(void *self) {
  dot_expr_edge_free(self);
}
LIST_DECLARE_STATIC_INLINE(list_expr_edge, dot_expr_edge, container_cmp_false,
                           container_new_move, container_delete_expr_edge);

// BLOCK
typedef struct dot_block_edge_struct {
  size_t id_from;
  size_t id_to;
  char  *label;
  char  *color;
} dot_block_edge;

static void dot_block_edge_free(dot_block_edge *self);

typedef struct dot_block_struct {
  const eir_block_base *ptr;
  size_t                expr_cnt;
  list_expr_edge       *expr_edges;
} dot_block;

static void dot_block_free(dot_block *self);

static inline int container_cmp_dot_block(const void *_lsv, const void *_rsv) {
  const dot_block *lsv = _lsv;
  const dot_block *rsv = _rsv;
  return container_cmp_ptr(lsv->ptr, rsv->ptr);
}
static inline size_t container_hash_dot_block(const void *_lsv) {
  const dot_block *lsv = _lsv;
  return container_hash_ptr(lsv->ptr);
}
static inline void container_delete_dot_block(void *_data) {
  dot_block_free(_data);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_block, dot_block, container_cmp_dot_block,
                              container_new_move, container_delete_dot_block,
                              container_hash_dot_block);

static inline void container_delete_dot_block_edge(void *data) {
  dot_block_edge_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_block_edge, dot_block_edge, container_cmp_false,
                           container_new_move, container_delete_dot_block_edge);

typedef struct dot_ctx_cfg_struct {
  hashset_block   *blocks; // visited
  list_block_edge *block_edges;
} dot_ctx_cfg;

// SUBROUTINE
typedef struct dot_subroutine_struct {
  const eir_subroutine *ptr;
  size_t                id;
  hashset_block        *blocks; // visited
} dot_subroutine;

static void dot_subroutine_free(dot_subroutine *self);

static inline int container_cmp_dot_subroutine(const void *_lsv,
                                               const void *_rsv) {
  const dot_subroutine *lsv = _lsv;
  const dot_subroutine *rsv = _rsv;
  return container_cmp_ptr(lsv->ptr, rsv->ptr);
}
static inline void container_delete_dot_subroutine(void *_lsv) {
  dot_subroutine_free(_lsv);
}
static inline size_t container_hash_dot_subroutine(const void *_lsv) {
  const dot_subroutine *lsv = _lsv;
  return container_hash_ptr(lsv->ptr);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_subroutine, dot_subroutine,
                              container_cmp_dot_subroutine, container_new_move,
                              container_delete_dot_subroutine,
                              container_hash_dot_subroutine);

typedef struct dot_subroutine_edge_struct {
  size_t id_from;
  size_t id_to;
} dot_subroutine_edge;

static void dot_subroutine_edge_free(dot_subroutine_edge *self);

static void container_delete_dot_subroutine_edge(void *self) {
  dot_subroutine_edge_free(self);
}
LIST_DECLARE_STATIC_INLINE(list_subroutine_edge, dot_subroutine_edge,
                           container_cmp_false, container_new_move,
                           container_delete_dot_subroutine_edge);

typedef struct subsymbol_struct {
  const hir_symbol     *symbol;
  const eir_subroutine *subroutine;
} subsymbol;

static inline subsymbol *subsymbol_new(const hir_symbol     *symbol,
                                       const eir_subroutine *sub) {
  subsymbol *self = MALLOC(subsymbol);
  self->symbol = symbol;
  self->subroutine = sub;
  return self;
}

static inline int container_cmp_subsymbol(const void *_lsv, const void *_rsv) {
  const subsymbol *lsv = _lsv;
  const subsymbol *rsv = _rsv;
  return container_cmp_ptr(lsv->symbol, rsv->symbol);
}
static inline void container_delete_subsymbol(void *data) {
  container_delete_ptr(data);
}
static inline size_t container_hash_subsymbol(const void *_data) {
  const subsymbol *data = _data;
  return container_hash_ptr(data->symbol);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_subsymbol, subsymbol,
                              container_cmp_subsymbol, container_new_move,
                              container_delete_subsymbol,
                              container_hash_subsymbol);

typedef struct dot_ctx_cg_struct {
  size_t                cnt;
  hashset_subroutine   *subroutines;
  list_subroutine_edge *subroutine_edges;
  hashset_subsymbol    *subindex;
} dot_ctx_cg;

// // DOT_EXPR
static void dot_expr_edge_free(dot_expr_edge *self) { free(self); }

static dot_expr_edge *dot_expr_edge_new(size_t id_from, size_t id_to) {
  dot_expr_edge *self = MALLOC(dot_expr_edge);
  self->id_to = id_to;
  self->id_from = id_from;
  return self;
}

// DOT_block
static void dot_block_free(dot_block *self) {
  list_expr_edge_free(self->expr_edges);
  free(self);
}

static dot_block *dot_block_new(const eir_block_base *ptr) {
  dot_block *self = MALLOC(dot_block);
  self->ptr = ptr;
  self->expr_cnt = 0;
  self->expr_edges = list_expr_edge_new();
  return self;
}

static dot_block_edge *dot_block_edge_new(size_t id_from, size_t id_to,
                                          char *label, char *color) {
  dot_block_edge *self = MALLOC(dot_block_edge);
  self->id_to = id_to;
  self->id_from = id_from;
  self->label = label;
  self->color = color;
  return self;
}

static void dot_block_edge_free(dot_block_edge *self) { free(self); }

// // CFG
static dot_ctx_cfg *ctx_cfg_new() {
  dot_ctx_cfg *cfg = MALLOC(dot_ctx_cfg);
  cfg->blocks = hashset_block_new();
  cfg->block_edges = list_block_edge_new();
  return cfg;
}

static void ctx_cfg_free(dot_ctx_cfg *cfg) {
  hashset_block_free(cfg->blocks);
  list_block_edge_free(cfg->block_edges);
  free(cfg);
}

static size_t cfg_traverse_expr_traverse(dot_block *st, string_buffer *buffer,
                                         eir_expr_base *expr) {
  char buf[64];

  if (!expr) {
    return 0;
  }

  size_t id = ++st->expr_cnt;
  sprintf(buf, "expr_%zu_%zu[", st->ptr->id, id);

  string_buffer_append(buffer, buf);

  int ok = 1;

  // print label
  switch (expr->kind) {
  case EIR_EXPR_UNARY: {
    eir_expr_unary *unary = (eir_expr_unary *)expr;
    snprintf(buf, sizeof(buf) - 1, "label=\"%s\"",
             eir_expr_unary_enum_str(unary->op));
    break;
  }
  case EIR_EXPR_BINARY: {
    eir_expr_binary *binary = (eir_expr_binary *)expr;
    snprintf(buf, sizeof(buf) - 1, "label=\"%s\"",
             eir_expr_binary_enum_str(binary->op));
    break;
  }
  case EIR_EXPR_LITERAL: {
    eir_expr_lit *lit = (eir_expr_lit *)expr;
    char         *ts = type_str(lit->lit->type);
    char         *vs = hir_lit_value_str(lit->lit);
    snprintf(buf, sizeof(buf) - 1, "label=\"(%s)%s\"", ts, vs);
    free(vs);
    free(ts);
    break;
  }
  case EIR_EXPR_IDENTIFIER: {
    eir_expr_id *id = (eir_expr_id *)expr;
    snprintf(buf, sizeof(buf) - 1, "label=\"%s\"", id->binding->name);
    break;
  }
  case EIR_EXPR_CALL: {
    eir_expr_call *call = (eir_expr_call *)expr;
    UNUSED(call);
    snprintf(buf, sizeof(buf) - 1, "label=\"CALL\"");
    break;
  }
  case EIR_EXPR_INDEX: {
    eir_expr_index *index = (eir_expr_index *)expr;
    UNUSED(index);
    snprintf(buf, sizeof(buf) - 1, "label=\"INDEX\"");
    break;
  }
  case EIR_EXPR_BUILTIN: {
    eir_expr_builtin *builtin = (eir_expr_builtin *)expr;
    char             *ts = type_str(builtin->base.type);
    const char       *kind = eir_expr_builtin_enum_str(builtin->kind);

    if (ts) {
      snprintf(buf, sizeof(buf) - 1, "label=\"BUILTIN_%s<%s>\"", kind, ts);
      free(ts);
    } else {
      snprintf(buf, sizeof(buf) - 1, "label=\"BUILTIN_%s\"", kind);
    }

    break;
  }
  default:
    error("unknown expression kind %d %p", expr->kind, expr);
    ok = 0;
  }

  if (ok) {
    string_buffer_append(buffer, buf);
  }

  string_buffer_append(buffer, "];\n");

  // traverse
  switch (expr->kind) {
  case EIR_EXPR_UNARY: {
    eir_expr_unary *unary = (eir_expr_unary *)expr;
    size_t child = cfg_traverse_expr_traverse(st, buffer, unary->first);
    if (child) {
      list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, child));
    }
    break;
  }
  case EIR_EXPR_BINARY: {
    eir_expr_binary *binary = (eir_expr_binary *)expr;
    size_t child1 = cfg_traverse_expr_traverse(st, buffer, binary->first);
    if (child1) {
      list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, child1));
    }
    size_t child2 = cfg_traverse_expr_traverse(st, buffer, binary->second);
    if (child2) {
      list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, child2));
    }
    break;
  }
  case EIR_EXPR_LITERAL: {
    eir_expr_lit *lit = (eir_expr_lit *)expr;
    UNUSED(lit);
    break;
  }
  case EIR_EXPR_IDENTIFIER: {
    eir_expr_id *id = (eir_expr_id *)expr;
    UNUSED(id);
    break;
  }
  case EIR_EXPR_CALL: {
    eir_expr_call *call = (eir_expr_call *)expr;
    size_t         child = cfg_traverse_expr_traverse(st, buffer, call->callee);
    if (child) {
      list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, child));
    }
    for (list_eir_expr_it it = list_eir_expr_begin(call->args); !it.end(&it);
         it.next(&it)) {
      eir_expr_base *arg = it.get(&it); // NOTE: may be very bad
      size_t         arg_id = cfg_traverse_expr_traverse(st, buffer, arg);
      if (arg_id) {
        list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, arg_id));
      }
    }
    break;
  }
  case EIR_EXPR_INDEX: {
    eir_expr_index *index = (eir_expr_index *)expr;
    size_t child = cfg_traverse_expr_traverse(st, buffer, index->indexed);
    if (child) {
      list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, child));
    }
    for (list_eir_expr_it it = list_eir_expr_begin(index->args); !it.end(&it);
         it.next(&it)) {
      eir_expr_base *arg = it.get(&it); // NOTE: may be very bad
      size_t         arg_id = cfg_traverse_expr_traverse(st, buffer, arg);
      if (arg_id) {
        list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, arg_id));
      }
    }
    break;
  }
  case EIR_EXPR_BUILTIN: {
    eir_expr_builtin *builtin = (eir_expr_builtin *)expr;
    for (list_eir_expr_it it = list_eir_expr_begin(builtin->args); !it.end(&it);
         it.next(&it)) {
      eir_expr_base *arg = it.get(&it); // NOTE: may be very bad
      size_t         arg_id = cfg_traverse_expr_traverse(st, buffer, arg);
      if (arg_id) {
        list_expr_edge_push_back(st->expr_edges, dot_expr_edge_new(id, arg_id));
      }
    }
    break;
  }
  default:
    error("unknown expression kind");
  }

  return id;
}

static void cfg_traverse_expr_link(dot_block *st, string_buffer *buffer) {
  char buf[128];

  for (list_expr_edge_it it = list_expr_edge_begin(st->expr_edges);
       !it.end(&it); it.next(&it)) {
    dot_expr_edge *edge = it.get(&it);
    snprintf(buf, sizeof(buf) - 1, "expr_%zu_%zu -> expr_%zu_%zu;\n",
             st->ptr->id, edge->id_from, st->ptr->id, edge->id_to);
    string_buffer_append(buffer, buf);
  }
}

static void cfg_traverse_expr(dot_block *st, string_buffer *buffer,
                              eir_expr_base *root) {
  char buf[64];

  snprintf(buf, sizeof(buf) - 1, "cluster_%zu_expr", st->ptr->id);
  string_buffer_append(buffer, "subgraph ");
  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, "{\n"
                               "style=filled;\n"
                               "fillcolor=white;\n"
                               "\n"
                               "label=\"expressions\";"
                               "\n");

  size_t expr_id = cfg_traverse_expr_traverse(st, buffer, root);
  cfg_traverse_expr_link(st, buffer);

  string_buffer_append(buffer, "}\n");

  if (expr_id) {
    snprintf(buf, sizeof(buf) - 1, "block_%zu -> expr_%zu_%zu[style=invis];",
             st->ptr->id, st->ptr->id, expr_id);
    string_buffer_append(buffer, buf);
  }
}

static size_t cfg_traverse(dot_ctx_cfg *ctx, string_buffer *buffer,
                           eir_block_base *root, int v_expr) {
  char buf[64];

  if (!root) {
    return 0;
  }

  hashset_block_it it =
      hashset_block_find(ctx->blocks, &(dot_block){.ptr = root});

  if (!it.end(&it)) {
    return ((dot_block *)it.get(&it))->ptr->id;
  }

  dot_block *st;
  { st = dot_block_new(root); }

  hashset_block_insert(ctx->blocks, st);

  string_buffer_append(buffer, "subgraph ");
  snprintf(buf, sizeof(buf) - 1, "cluster_%zu", st->ptr->id);
  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, " {\n"
                               "style=\"filled,solid\";\n"
                               "shape=box;\n"
                               "color=black;\n"
                               "fillcolor=lightgray;\n"
                               "\n");

  snprintf(buf, sizeof(buf) - 1, "block_%zu", st->ptr->id);
  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, "[shape=plaintext]\n"
                               "\n");

  if (v_expr) {
    switch (root->kind) {
    case EIR_BLOCK_NEXT: {
      cfg_traverse_expr(st, buffer, ((eir_block_next *)root)->expr);
      break;
    }
    case EIR_BLOCK_COND: {
      cfg_traverse_expr(st, buffer, ((eir_block_cond *)root)->cond);
      break;
    }
    default:
      error("unknown expression type");
    }
  }

  string_buffer_append(buffer, "}\n\n");

  switch (root->kind) {
  case EIR_BLOCK_NEXT: {
    size_t child_id =
        cfg_traverse(ctx, buffer, ((eir_block_next *)root)->next, v_expr);
    if (child_id) {
      list_block_edge_push_back(
          ctx->block_edges,
          dot_block_edge_new(st->ptr->id, child_id, "", "black"));
    }
    break;
  }
  case EIR_BLOCK_COND: {
    size_t child_jz_id =
        cfg_traverse(ctx, buffer, ((eir_block_cond *)root)->jz, v_expr);
    if (child_jz_id) {
      list_block_edge_push_back(
          ctx->block_edges,
          dot_block_edge_new(st->ptr->id, child_jz_id, "False", "blue"));
    }

    size_t child_je_id =
        cfg_traverse(ctx, buffer, ((eir_block_cond *)root)->je, v_expr);
    if (child_je_id) {
      list_block_edge_push_back(
          ctx->block_edges,
          dot_block_edge_new(st->ptr->id, child_je_id, "True", "red"));
    }

    break;
  }
  default:
    error("unknown block type %d", root->kind);
  }

  return st->ptr->id;
}

static void cfg_link(dot_ctx_cfg *cfg, string_buffer *buffer) {
  char buf[256];
  for (list_block_edge_it it = list_block_edge_begin(cfg->block_edges);
       !it.end(&it); it.next(&it)) {
    dot_block_edge *edge = it.get(&it);
    snprintf(buf, sizeof(buf) - 1, "block_%zu -> block_%zu", edge->id_from,
             edge->id_to);
    string_buffer_append(buffer, buf);

    if (strlen(edge->label)) {
      char *escaped_label = escape_html(edge->label);
      snprintf(buf, sizeof(buf) - 1,
               "[color=%s,ltail=cluster_%zu,lhead=cluster_%zu,labelfloat=true,"
               "taillabel=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" "
               "CELLSPACING=\"0\"><TR><TD "
               "BGCOLOR=\"white\">%s</TD></TR></TABLE>>];\n",
               edge->color, edge->id_from, edge->id_to, escaped_label);
      free(escaped_label);
    } else {
      snprintf(
          buf, sizeof(buf) - 1,
          "[color=%s,ltail=cluster_%zu,lhead=cluster_%zu,labelfloat=true];\n",
          edge->color, edge->id_from, edge->id_to);
    }

    string_buffer_append(buffer, buf);
  }
  string_buffer_append(buffer, "\n");
}

dot_string *dot_eir_cfg(eir *eir, const eir_subroutine *sub, int v_add_expr) {
  UNUSED(eir);

  dot_ctx_cfg *cfg = ctx_cfg_new();

  string_buffer *buffer = string_buffer_new(0, 0);

  string_buffer_append(
      buffer, "strict digraph G {\n"
              "compound=true;\n"
              "graph [pad=\"0\",nodesep=\"1\",ranksep=\"0.75\",splines=true];\n"
              "node [shape=rect, style=filled];\n"
              "\n");

  cfg_traverse(cfg, buffer, sub->root, v_add_expr);
  cfg_link(cfg, buffer);

  string_buffer_append(buffer, "}");

  size_t len = string_buffer_size(buffer) / sizeof(char);
  char  *chars = string_buffer_detach(buffer);

  ctx_cfg_free(cfg);

  return dot_string_new(chars, len);
}

// CG
static dot_subroutine *dot_subroutine_new(const eir_subroutine *sub,
                                          size_t                id) {
  dot_subroutine *self = MALLOC(dot_subroutine);
  self->ptr = sub;
  self->id = id;
  self->blocks = hashset_block_new();
  return self;
}

static void dot_subroutine_free(dot_subroutine *self) {
  hashset_block_free(self->blocks);
  free(self);
}

static dot_subroutine_edge *dot_subroutine_edge_new(size_t id_from,
                                                    size_t id_to) {
  dot_subroutine_edge *self = MALLOC(dot_subroutine_edge);
  self->id_from = id_from;
  self->id_to = id_to;
  return self;
}

static void dot_subroutine_edge_free(dot_subroutine_edge *self) { free(self); }

static dot_ctx_cg *ctx_cg_new() {
  dot_ctx_cg *cg = MALLOC(dot_ctx_cg);
  cg->cnt = 0;
  cg->subroutines = hashset_subroutine_new();
  cg->subroutine_edges = list_subroutine_edge_new();
  cg->subindex = hashset_subsymbol_new();
  return cg;
}

static void ctx_cg_free(dot_ctx_cg *self) {
  hashset_subroutine_free(self->subroutines);
  list_subroutine_edge_free(self->subroutine_edges);
  hashset_subsymbol_free(self->subindex);
  free(self);
}

static size_t cg_traverse(dot_ctx_cg *ctx_cg, string_buffer *buffer,
                          const eir *eir, const eir_subroutine *sub);

static void cg_traverse_expr(dot_ctx_cg *ctx, dot_subroutine *dsub,
                             string_buffer *buffer, const eir *eir,
                             const eir_expr_base *expr) {
  if (!expr) {
    return;
  }

  switch (expr->kind) {
  case EIR_EXPR_UNARY: {
    eir_expr_unary *unary = (eir_expr_unary *)expr;
    cg_traverse_expr(ctx, dsub, buffer, eir, unary->first);
    break;
  }
  case EIR_EXPR_BINARY: {
    eir_expr_binary *binary = (eir_expr_binary *)expr;
    cg_traverse_expr(ctx, dsub, buffer, eir, binary->first);
    cg_traverse_expr(ctx, dsub, buffer, eir, binary->second);
    break;
  }
  case EIR_EXPR_LITERAL: {
    eir_expr_lit *lit = (eir_expr_lit *)expr;
    UNUSED(lit);
    break;
  }
  case EIR_EXPR_IDENTIFIER: {
    eir_expr_id *id = (eir_expr_id *)expr;
    UNUSED(id);
    break;
  }
  case EIR_EXPR_CALL: {
    eir_expr_call *call = (eir_expr_call *)expr;

    eir_expr_base *sub_base = call->callee;
    if (sub_base->kind != EIR_EXPR_IDENTIFIER) {
      debug("expression under load is not identifier");
      break;
    }
    eir_expr_id *sub_id = (eir_expr_id *)sub_base;

    hashset_subsymbol_it it = hashset_subsymbol_find(
        ctx->subindex, &(subsymbol){.symbol = sub_id->binding});

    if (it.end(&it)) {
      error("identifer is not found");
      break;
    }

    const eir_subroutine *sub_child = it.get(&it)->subroutine;
    size_t                child = cg_traverse(ctx, buffer, eir, sub_child);

    if (child) {
      list_subroutine_edge_push_back(ctx->subroutine_edges,
                                     dot_subroutine_edge_new(dsub->id, child));
    }
    break;
  }
  case EIR_EXPR_INDEX: {
    eir_expr_index *index = (eir_expr_index *)expr;
    cg_traverse_expr(ctx, dsub, buffer, eir, index->indexed);
    for (list_eir_expr_it it = list_eir_expr_begin(index->args); !it.end(&it);
         it.next(&it)) {
      eir_expr_base *arg = it.get(&it);
      cg_traverse_expr(ctx, dsub, buffer, eir, arg);
    }
    break;
  }
  case EIR_EXPR_BUILTIN: {
    eir_expr_builtin *builtin = (eir_expr_builtin *)expr;
    for (list_eir_expr_it it = list_eir_expr_begin(builtin->args); !it.end(&it);
         it.next(&it)) {
      eir_expr_base *arg = it.get(&it);
      cg_traverse_expr(ctx, dsub, buffer, eir, arg);
    }
    break;
  }
  default:
    error("unknown expression kind %d %p", expr->kind, expr);
  }
}

static void cg_traverse_block(dot_ctx_cg *ctx_cg, dot_subroutine *dsub,
                              string_buffer *buffer, const eir *eir,
                              const eir_block_base *root) {
  if (!root) {
    return;
  }

  hashset_block_it it =
      hashset_block_find(dsub->blocks, &(dot_block){.ptr = root});

  if (!it.end(&it)) {
    return;
  }

  // searches by ptr
  dot_block *dbl = dot_block_new(root);
  hashset_block_insert(dsub->blocks, dbl);

  // traverse current block
  switch (root->kind) {
  case EIR_BLOCK_NEXT: {
    cg_traverse_expr(ctx_cg, dsub, buffer, eir, ((eir_block_next *)root)->expr);
    break;
  }
  case EIR_BLOCK_COND: {
    cg_traverse_expr(ctx_cg, dsub, buffer, eir, ((eir_block_cond *)root)->cond);
    break;
  }
  default:
    error("unknown expression type");
  }

  // traverse other blocks
  switch (root->kind) {
  case EIR_BLOCK_NEXT:
    cg_traverse_block(ctx_cg, dsub, buffer, eir,
                      ((eir_block_next *)root)->next);
    break;
  case EIR_BLOCK_COND:
    cg_traverse_block(ctx_cg, dsub, buffer, eir, ((eir_block_cond *)root)->jz);
    cg_traverse_block(ctx_cg, dsub, buffer, eir, ((eir_block_cond *)root)->je);
    break;
  default:
    error("unknown block type %d", root->kind);
  }
}

static size_t cg_traverse(dot_ctx_cg *ctx, string_buffer *buffer,
                          const eir *eir, const eir_subroutine *sub) {
  char buf[64];
  if (!sub) {
    return 0;
  }

  hashset_subroutine_it it = hashset_subroutine_find(
      ctx->subroutines, &(dot_subroutine){.ptr = (eir_subroutine *)sub});

  if (!it.end(&it)) {
    return ((dot_subroutine *)it.get(&it))->id;
  }

  dot_subroutine *dsub;
  {
    size_t sub_id = ++ctx->cnt;
    dsub = dot_subroutine_new(sub, sub_id);
  }

  hashset_subroutine_insert(ctx->subroutines, dsub);

  snprintf(buf, sizeof(buf) - 1, "subroutine_%zu[label=%s];\n", dsub->id,
           sub->binding->name);
  string_buffer_append(buffer, buf);

  // find function calls in blocks
  cg_traverse_block(ctx, dsub, buffer, eir, sub->root);

  return dsub->id;
}

static void cg_link(dot_ctx_cg *ctx, string_buffer *buffer) {
  char buf[64];
  for (list_subroutine_edge_it it =
           list_subroutine_edge_begin(ctx->subroutine_edges);
       !it.end(&it); it.next(&it)) {
    dot_subroutine_edge *edge = it.get(&it);
    snprintf(buf, sizeof(buf) - 1, "subroutine_%zu -> subroutine_%zu;\n",
             edge->id_from, edge->id_to);
    string_buffer_append(buffer, buf);
  }
}

dot_string *dot_eir_cg(const eir *eir, const eir_subroutine *sub) {
  string_buffer *buffer = string_buffer_new(0, 0);

  dot_ctx_cg *ctx = ctx_cg_new();

  for (list_eir_subroutine_it it = list_eir_subroutine_begin(eir->subroutines);
       !it.end(&it); it.next(&it)) {
    eir_subroutine *it_sub = it.get(&it);
    hashset_subsymbol_insert(ctx->subindex,
                             subsymbol_new(it_sub->binding, it_sub));
  }

  string_buffer_append(
      buffer, "strict digraph G {\n"
              "compound=true;\n"
              "graph [pad=\"0\",nodesep=\"1\",ranksep=\"0.75\",splines=true];\n"
              "node [shape=rect, style=filled];\n"
              "\n");

  cg_traverse(ctx, buffer, eir, sub);
  cg_link(ctx, buffer);

  string_buffer_append(buffer, "}");

  ctx_cg_free(ctx);

  size_t len = string_buffer_size(buffer) / sizeof(char);
  char  *chars = string_buffer_detach(buffer);

  return dot_string_new(chars, len);
}
