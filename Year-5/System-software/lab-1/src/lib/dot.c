#include "dot.h"

#include "util/list.h"
#include "util/list_funcs.h"
#include "util/string.h"

#include <antlr3.h>
#include <antlr3string.h>
#include <assert.h>

struct dot_ctx {
  const char *graph;
  const char *node_style;

  size_t id;

  p_list list_nodes; // key - id, value - label
  p_list list_edges; // key - id_u, value - id_v

  void (*free)(struct dot_ctx *);
};

void dot_ctx_free(struct dot_ctx *ctx) {
  free((void *)ctx->graph);
  free((void *)ctx->node_style);

  ctx->list_nodes->free(ctx->list_nodes);
  ctx->list_edges->free(ctx->list_edges);

  free(ctx);
}

struct dot_ctx *dot_ctx_new(const char *graph, const char *node_style) {
  struct dot_ctx *ctx = malloc(sizeof(struct dot_ctx));

  ctx->graph = strdup(graph);
  ctx->node_style = strdup(node_style);

  ctx->id = 0;

  ctx->list_nodes =
      list_new(string_equals, string_string_new, string_string_delete, 1);
  ctx->list_edges =
      list_new(string_equals, string_string_new, string_string_delete, 0);

  ctx->free = dot_ctx_free;

  return ctx;
}

size_t dot_len(struct dot_ctx *ctx) {
  size_t len = 0;

  len += strlen(ctx->graph);
  len += strlen("{\n");
  len += strlen("node []\n");
  len += strlen(ctx->node_style);

  for (list_iter it = ctx->list_nodes->head; it != NULL; it = it->next) {
    len += strlen(it->data->key);
    len += strlen("[label=]\n");
    len += strlen(it->data->value);
  }

  for (list_iter it = ctx->list_edges->head; it != NULL; it = it->next) {
    len += strlen(it->data->key);
    len += strlen(" -> \n");
    len += strlen(it->data->value);
  }

  len += strlen("}");

  return len;
}

void dot_generate(struct dot_ctx *ctx, char *str) {
  str[0] = '\0';

  char *end = stpcpy(str, ctx->graph);
  end = stpcpy(end, "{\n");
  end = stpcpy(end, "node [");
  end = stpcpy(end, ctx->node_style);
  end = stpcpy(end, "]\n");

  for (list_iter it = ctx->list_nodes->head; it != NULL; it = it->next) {
    end = stpcpy(end, it->data->key);
    end = stpcpy(end, "[label=");
    end = stpcpy(end, it->data->value);
    end = stpcpy(end, "]\n");
  }

  for (list_iter it = ctx->list_edges->head; it != NULL; it = it->next) {
    end = stpcpy(end, it->data->key);
    end = stpcpy(end, " -> ");
    end = stpcpy(end, it->data->value);
    end = stpcpy(end, "\n");
  }

  end = stpcpy(end, "}");
}

size_t dot_dfs(struct dot_ctx *ctx, pANTLR3_BASE_TREE root) {
  if (root == NULL) {
    return 0;
  }

  char label[256];
  pANTLR3_COMMON_TOKEN root_token = root->getToken(root);
  if (root_token == NULL) {
    snprintf(label, 255,
             "< <FONT POINT-SIZE=\"16\"><b>&lt;empty&gt;</b></FONT> >");
  } else {
    char *str = (char *)root_token->getText(root_token)->chars;

    char *escaped_str = escape_html(str);

    snprintf(label, 255,
             "< <FONT POINT-SIZE=\"16\"><b>%s</b></FONT><br/><FONT "
             "POINT-SIZE=\"12\">Line: %d, Pos: %d</FONT> >",
             escaped_str, root_token->line,
             root_token->getCharPositionInLine(root_token));

    free(escaped_str);
  }

  size_t id = ++ctx->id;
  char id_s[32];
  sprintf(id_s, "id_%zu", id);

  ctx->list_nodes->put(ctx->list_nodes, id_s, label);

  for (size_t i = 0; i < root->getChildCount(root); ++i) {
    pANTLR3_BASE_TREE child = root->getChild(root, i);
    size_t child_id = dot_dfs(ctx, child);

    if (child_id != 0) {
      char child_id_s[32];
      sprintf(child_id_s, "id_%zu", child_id);

      ctx->list_edges->put(ctx->list_edges, id_s, child_id_s);
    }
  }

  return id;
}

struct dot_ctx *dot_new(pANTLR3_BASE_TREE tree) {
  struct dot_ctx *ctx =
      dot_ctx_new("strict digraph AST", "shape=rect, style=filled");

  dot_dfs(ctx, tree);

  return ctx;
}

dot_string dot_ast(pANTLR3_BASE_TREE tree) {
  struct dot_ctx *ctx = dot_new(tree);

  size_t len = dot_len(ctx);
  char *chars = malloc(len + 1);

  dot_generate(ctx, chars);

  assert(len == strlen(chars));

  ctx->free(ctx);

  return (dot_string){.len = len, .chars = chars};
}
