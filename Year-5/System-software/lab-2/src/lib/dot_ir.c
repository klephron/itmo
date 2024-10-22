#include "dot_ir.h"
#include "util/container_util.h"
#include "util/log.h"
#include "util/macro.h"
#include "util/string.h"
#include "util/string_buffer.h"

#include <stdlib.h>
#include <string.h>

struct dot_statement_edge {
  size_t id_from;
  size_t id_to;
  char  *label;
  char  *color;
};

struct dot_statement {
  ir_block_base *ptr;
  size_t         id;
};

struct dot_ctx_cfg {
  size_t    id;
  p_hashset blocks;
  p_list    statement_edges;
};

// DOT_STATEMENT
static void dot_statement_free(struct dot_statement *self) { free(self); }

static struct dot_statement *dot_statement_new(ir_block_base *ptr, size_t id) {
  struct dot_statement *self = MALLOC(struct dot_statement);
  self->ptr = ptr;
  self->id = id;
  return self;
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

// CONTAINER
static void container_delete_statement_edge(void *_data) {
  dot_statement_edge_free(_data);
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

// CFG
static struct dot_ctx_cfg *ctx_cfg_new() {
  struct dot_ctx_cfg *cfg = MALLOC(struct dot_ctx_cfg);
  cfg->id = 0;
  cfg->blocks =
      hashset_new(container_cmp_dot_statement, container_new_move,
                  container_delete_dot_statement, container_hash_dot_statement);
  cfg->statement_edges = list_new(container_cmp_ptr, container_new_move,
                                  container_delete_statement_edge);
  return cfg;
}

static void ctx_cfg_free(struct dot_ctx_cfg *cfg) {
  cfg->id = 0;
  hashset_free(cfg->blocks);
  list_free(cfg->statement_edges);
  free(cfg);
}

static size_t cfg_traverse(struct dot_ctx_cfg *cfg, string_buffer *buffer,
                           ir_block_base *root) {
  char buf[64];

  if (!root) {
    return 0;
  }

  hashset_it it =
      hashset_find(cfg->blocks, &(struct dot_statement){.ptr = root});

  if (!it.end(&it)) {
    return ((struct dot_statement *)it.get(&it))->id;
  }

  size_t statement_id = ++cfg->id;

  hashset_insert(cfg->blocks, dot_statement_new(root, statement_id));

  string_buffer_append(buffer, "subgraph ");
  snprintf(buf, sizeof(buf) - 1, "cluster_%zu", statement_id);

  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, " {\n"
                               "style=\"filled,solid\";\n"
                               "shape=box;\n"
                               "color=black;\n"
                               "fillcolor=lightgray;\n"
                               "\n");

  snprintf(buf, sizeof(buf) - 1, "statement_%zu", statement_id);
  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, "[shape=plaintext]\n"
                               "\n");
  // TODO: add expressions

  string_buffer_append(buffer, "}\n\n");

  switch (root->kind) {
  case IR_BLOCK_NEXT: {
    size_t child_id = cfg_traverse(cfg, buffer, ((ir_block_next *)root)->next);
    if (child_id) {
      list_push_back(
          cfg->statement_edges,
          dot_statement_edge_new(statement_id, child_id, "", "black"));
    }
    break;
  }
  case IR_BLOCK_COND: {
    size_t child_jz_id = cfg_traverse(cfg, buffer, ((ir_block_cond *)root)->jz);
    if (child_jz_id) {
      list_push_back(
          cfg->statement_edges,
          dot_statement_edge_new(statement_id, child_jz_id, "False", "blue"));
    }

    size_t child_je_id = cfg_traverse(cfg, buffer, ((ir_block_cond *)root)->je);
    if (child_je_id) {
      list_push_back(
          cfg->statement_edges,
          dot_statement_edge_new(statement_id, child_je_id, "True", "red"));
    }

    break;
  }
  default:
    error("unknown block type %d", root->kind);
  }

  return statement_id;
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

dot_string *dot_ir_cfg(const ir_ctx *ctx, const ir_subroutine *sub) {
  UNUSED(ctx);

  struct dot_ctx_cfg *cfg = ctx_cfg_new();

  string_buffer *buffer = string_buffer_new(0, 0);

  string_buffer_append(
      buffer, "strict digraph G {\n"
              "compound=true;\n"
              "graph [pad=\"0\",nodesep=\"1\",ranksep=\"1\",splines=true];\n"
              "node [shape=rect, style=filled];\n"
              "\n");

  cfg_traverse(cfg, buffer, sub->cfg);
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
