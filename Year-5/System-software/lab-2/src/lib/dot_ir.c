#include "dot_ir.h"
#include "util/container_util.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"
#include "util/string_buffer.h"

#include <stdlib.h>
#include <string.h>

struct dot_expr_edge {
  size_t id_from;
  size_t id_to;
};

struct dot_statement_edge {
  size_t id_from;
  size_t id_to;
  char  *label;
  char  *color;
};

struct dot_statement {
  ir_block_base *ptr;
  size_t         id;
  size_t         expr_cnt;
  p_list         expr_edges;
};

struct dot_ctx_cfg {
  size_t    cnt;
  p_hashset blocks;
  p_list    statement_edges;
};

// DOT_EXPR
static void dot_expr_edge_free(struct dot_expr_edge *self) { free(self); }

static struct dot_expr_edge *dot_expr_edge_new(size_t id_from, size_t id_to) {
  struct dot_expr_edge *self = MALLOC(struct dot_expr_edge);
  self->id_to = id_to;
  self->id_from = id_from;
  return self;
}

static void container_delete_expr_edge(void *self) { dot_expr_edge_free(self); }

// DOT_STATEMENT
static void dot_statement_free(struct dot_statement *self) {
  list_free(self->expr_edges);
  free(self);
}

static struct dot_statement *dot_statement_new(ir_block_base *ptr, size_t id) {
  struct dot_statement *self = MALLOC(struct dot_statement);
  self->ptr = ptr;
  self->id = id;
  self->expr_cnt = 0;
  self->expr_edges = list_new(container_cmp_false, container_new_move,
                              container_delete_expr_edge);
  return self;
}

static int container_cmp_dot_statement(const void *_lsv, const void *_rsv) {
  const struct dot_statement *lsv = _lsv;
  const struct dot_statement *rsv = _rsv;
  return container_cmp_ptr(lsv->ptr, rsv->ptr);
}

static size_t container_hash_dot_statement(const void *_lsv) {
  const struct dot_statement *lsv = _lsv;
  return container_hash_ptr(lsv->ptr);
}
static void container_delete_dot_statement(void *_data) {
  dot_statement_free(_data);
}

// DOT_STATEMENT_EDGE
static struct dot_statement_edge *
dot_statement_edge_new(size_t id_from, size_t id_to, char *label, char *color) {
  struct dot_statement_edge *self = MALLOC(struct dot_statement_edge);
  self->id_to = id_to;
  self->id_from = id_from;
  self->label = label;
  self->color = color;
  return self;
}

static void dot_statement_edge_free(struct dot_statement_edge *self) {
  free(self);
}

static void container_delete_statement_edge(void *_data) {
  dot_statement_edge_free(_data);
}

// CFG
static struct dot_ctx_cfg *ctx_cfg_new() {
  struct dot_ctx_cfg *cfg = MALLOC(struct dot_ctx_cfg);
  cfg->cnt = 0;
  cfg->blocks =
      hashset_new(container_cmp_dot_statement, container_new_move,
                  container_delete_dot_statement, container_hash_dot_statement);
  cfg->statement_edges = list_new(container_cmp_ptr, container_new_move,
                                  container_delete_statement_edge);
  return cfg;
}

static void ctx_cfg_free(struct dot_ctx_cfg *cfg) {
  cfg->cnt = 0;
  hashset_free(cfg->blocks);
  list_free(cfg->statement_edges);
  free(cfg);
}

static size_t cfg_traverse_expr_traverse(struct dot_statement *st,
                                         string_buffer        *buffer,
                                         ir_expr_base         *expr) {
  char buf[64];

  if (!expr) {
    return 0;
  }

  size_t id = ++st->expr_cnt;
  sprintf(buf, "expr_%zu_%zu[", st->id, id);

  string_buffer_append(buffer, buf);

  int ok = 1;

  // print label
  switch (expr->kind) {
  case IR_EXPR_UNARY: {
    ir_expr_unary *unary = (ir_expr_unary *)expr;
    snprintf(buf, sizeof(buf) - 1, "label=\"unary,op=%d\"", unary->op);
    break;
  }
  case IR_EXPR_BINARY: {
    ir_expr_binary *binary = (ir_expr_binary *)expr;
    snprintf(buf, sizeof(buf) - 1, "label=\"binary,op=%d\"", binary->op);
    break;
  }
  case IR_EXPR_LITERAL: {
    ir_expr_lit *lit = (ir_expr_lit *)expr;
    snprintf(buf, sizeof(buf) - 1, "label=\"lit,type=%d\"", lit->lit->type);
    break;
  }
  case IR_EXPR_IDENTIFIER: {
    ir_expr_id *id = (ir_expr_id *)expr;
    snprintf(buf, sizeof(buf) - 1, "label=\"lit,name=%s\"", id->id->name);
    break;
  }
  case IR_EXPR_CALL: {
    ir_expr_call *call = (ir_expr_call *)expr;
    UNUSED(call);
    snprintf(buf, sizeof(buf) - 1, "label=\"call\"");
    break;
  }
  case IR_EXPR_INDEX: {
    ir_expr_index *index = (ir_expr_index *)expr;
    UNUSED(index);
    snprintf(buf, sizeof(buf) - 1, "label=\"index\"");
    break;
  }
  default:
    error("unknown expression kind");
    ok = 0;
  }

  if (ok) {
    string_buffer_append(buffer, buf);
  }

  string_buffer_append(buffer, "];\n");

  // traverse
  switch (expr->kind) {
  case IR_EXPR_UNARY: {
    ir_expr_unary *unary = (ir_expr_unary *)expr;
    size_t         child = cfg_traverse_expr_traverse(st, buffer, unary->first);
    if (child) {
      list_push_back(st->expr_edges, dot_expr_edge_new(id, child));
    }
    break;
  }
  case IR_EXPR_BINARY: {
    ir_expr_binary *binary = (ir_expr_binary *)expr;
    size_t child1 = cfg_traverse_expr_traverse(st, buffer, binary->first);
    if (child1) {
      list_push_back(st->expr_edges, dot_expr_edge_new(id, child1));
    }
    size_t child2 = cfg_traverse_expr_traverse(st, buffer, binary->second);
    if (child2) {
      list_push_back(st->expr_edges, dot_expr_edge_new(id, child2));
    }
    break;
  }
  case IR_EXPR_LITERAL: {
    ir_expr_lit *lit = (ir_expr_lit *)expr;
    UNUSED(lit);
    break;
  }
  case IR_EXPR_IDENTIFIER: {
    ir_expr_id *id = (ir_expr_id *)expr;
    UNUSED(id);
    break;
  }
  case IR_EXPR_CALL: {
    ir_expr_call *call = (ir_expr_call *)expr;
    size_t        child = cfg_traverse_expr_traverse(st, buffer, call->callee);
    if (child) {
      list_push_back(st->expr_edges, dot_expr_edge_new(id, child));
    }
    for (list_it it = call->args->head; it; it = it->next) {
      ir_expr_base *arg = it->data; // NOTE: may be very bad
      size_t        arg_id = cfg_traverse_expr_traverse(st, buffer, arg);
      if (arg_id) {
        list_push_back(st->expr_edges, dot_expr_edge_new(id, arg_id));
      }
    }
    break;
  }
  case IR_EXPR_INDEX: {
    ir_expr_index *index = (ir_expr_index *)expr;
    size_t child = cfg_traverse_expr_traverse(st, buffer, index->indexed);
    if (child) {
      list_push_back(st->expr_edges, dot_expr_edge_new(id, child));
    }
    for (list_it it = index->args->head; it; it = it->next) {
      ir_expr_base *arg = it->data; // NOTE: may be very bad
      size_t        arg_id = cfg_traverse_expr_traverse(st, buffer, arg);
      if (arg_id) {
        list_push_back(st->expr_edges, dot_expr_edge_new(id, arg_id));
      }
    }
    break;
  }
  default:
    error("unknown expression kind");
  }

  return id;
}

static void cfg_traverse_expr_link(struct dot_statement *st,
                                   string_buffer        *buffer) {
  char buf[128];

  for (list_it it = st->expr_edges->head; it; it = it->next) {
    struct dot_expr_edge *edge = it->data;
    snprintf(buf, sizeof(buf) - 1, "expr_%zu_%zu -> expr_%zu_%zu;\n", st->id,
             edge->id_from, st->id, edge->id_to);
    string_buffer_append(buffer, buf);
  }
}

static void cfg_traverse_expr(struct dot_statement *st, string_buffer *buffer,
                              ir_expr_base *root) {
  char buf[64];

  snprintf(buf, sizeof(buf) - 1, "cluster_%zu_expr", st->id);
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
    snprintf(buf, sizeof(buf) - 1,
             "statement_%zu -> expr_%zu_%zu[style=invis];", st->id, st->id,
             expr_id);
    string_buffer_append(buffer, buf);
  }
}

static size_t cfg_traverse(struct dot_ctx_cfg *cfg, string_buffer *buffer,
                           ir_block_base *root, int v_expr) {
  char buf[64];

  if (!root) {
    return 0;
  }

  hashset_it it =
      hashset_find(cfg->blocks, &(struct dot_statement){.ptr = root});

  if (!it.end(&it)) {
    return ((struct dot_statement *)it.get(&it))->id;
  }

  struct dot_statement *st;
  {
    size_t statement_id = ++cfg->cnt;
    st = dot_statement_new(root, statement_id);
  }

  hashset_insert(cfg->blocks, st);

  string_buffer_append(buffer, "subgraph ");
  snprintf(buf, sizeof(buf) - 1, "cluster_%zu", st->id);

  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, " {\n"
                               "style=\"filled,solid\";\n"
                               "shape=box;\n"
                               "color=black;\n"
                               "fillcolor=lightgray;\n"
                               "\n");

  snprintf(buf, sizeof(buf) - 1, "statement_%zu", st->id);
  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, "[shape=plaintext]\n"
                               "\n");

  if (v_expr) {
    switch (root->kind) {
    case IR_BLOCK_NEXT: {
      cfg_traverse_expr(st, buffer, ((ir_block_next *)root)->expr);
      break;
    }
    case IR_BLOCK_COND: {
      cfg_traverse_expr(st, buffer, ((ir_block_cond *)root)->cond);
      break;
    }
    default:
      error("unknown expression type");
    }
  }

  string_buffer_append(buffer, "}\n\n");

  switch (root->kind) {
  case IR_BLOCK_NEXT: {
    size_t child_id =
        cfg_traverse(cfg, buffer, ((ir_block_next *)root)->next, v_expr);
    if (child_id) {
      list_push_back(cfg->statement_edges,
                     dot_statement_edge_new(st->id, child_id, "", "black"));
    }
    break;
  }
  case IR_BLOCK_COND: {
    size_t child_jz_id =
        cfg_traverse(cfg, buffer, ((ir_block_cond *)root)->jz, v_expr);
    if (child_jz_id) {
      list_push_back(
          cfg->statement_edges,
          dot_statement_edge_new(st->id, child_jz_id, "False", "blue"));
    }

    size_t child_je_id =
        cfg_traverse(cfg, buffer, ((ir_block_cond *)root)->je, v_expr);
    if (child_je_id) {
      list_push_back(
          cfg->statement_edges,
          dot_statement_edge_new(st->id, child_je_id, "True", "red"));
    }

    break;
  }
  default:
    error("unknown block type %d", root->kind);
  }

  return st->id;
}

static void cfg_link(struct dot_ctx_cfg *cfg, string_buffer *buffer) {
  char buf[256];
  for (list_it it = cfg->statement_edges->head; it; it = it->next) {
    struct dot_statement_edge *edge = it->data;
    snprintf(buf, sizeof(buf) - 1, "statement_%zu -> statement_%zu",
             edge->id_from, edge->id_to);
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
      snprintf(buf, sizeof(buf) - 1,
               "[color=%s,ltail=cluster_%zu,lhead=cluster_%zu];\n", edge->color,
               edge->id_from, edge->id_to);
    }

    string_buffer_append(buffer, buf);
  }
  string_buffer_append(buffer, "\n");
}

dot_string *dot_ir_cfg(const ir_ctx *ctx, const ir_subroutine *sub,
                       int v_expr) {
  UNUSED(ctx);

  struct dot_ctx_cfg *cfg = ctx_cfg_new();

  string_buffer *buffer = string_buffer_new(0, 0);

  string_buffer_append(
      buffer, "strict digraph G {\n"
              "compound=true;\n"
              "graph [pad=\"0\",nodesep=\"1\",ranksep=\"1\",splines=true];\n"
              "node [shape=rect, style=filled];\n"
              "\n");

  cfg_traverse(cfg, buffer, sub->cfg, v_expr);
  cfg_link(cfg, buffer);

  string_buffer_append(buffer, "}");

  size_t len = string_buffer_size(buffer) / sizeof(char);
  char  *chars = string_buffer_detach(buffer);

  ctx_cfg_free(cfg);

  return dot_string_new(chars, len);
}

// CG
dot_string *dot_ir_cg(const ir_ctx *ctx, const ir_subroutine *sub) {
  size_t len = 10;
  char  *chars = malloc(len);

  return dot_string_new(chars, len);
}
