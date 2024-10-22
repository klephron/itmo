#include "dot_ir.h"
#include "util/string_buffer.h"

#include <stdlib.h>

struct dot_statement_edge {
  char *id_from;
  char *id_to;
  char *label;
  char *color;
};

struct dot_statement {
  char *id;
  char *label;
};

struct dot_ctx {
  const char *graph;
  const char *node_style;

  size_t id;

  p_list statements;
  p_list statement_edges;
};

dot_string *dot_ir_cfg(const ir_ctx *ctx, const ir_subroutine *sub) {
  string_buffer *buffer = string_buffer_new(0, 0);

  string_buffer_append(buffer, "strict digraph G {\n"
                               "compound=true;\n"
                               "node [shape=rect, style=filled];\n"
                               "\n");

  string_buffer_append(buffer, "}");

  size_t len = string_buffer_size(buffer) / sizeof(char);
  char  *chars = string_buffer_detach(buffer);
  return dot_string_new(chars, len);
}

dot_string *dot_ir_cg(const ir_ctx *ctx, const ir_subroutine *sub) {
  size_t len = 10;
  char  *chars = malloc(len);

  return dot_string_new(chars, len);
}
