#pragma once

#include "ir/expr.h"
#include "ir/pass.h"
#include "ir/terminal.h"

struct ir_pass2 {
  struct ir_pass_base base;
  struct ir_ctx      *ctx;
  const char         *file_name;
};

typedef struct ir_pass2 ir_pass2;

void ir_pass2_init(ir_pass2 *self, struct ir_ctx *ctx, const char *file_name);

ir_term_lit *ir_pass2_str(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_rune(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_hex(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_bits(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_dec(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_bool(ir_pass2 *self, const char *str);

ir_term_id    *ir_pass2_term_id_new(ir_pass2 *self, const char *id);
ir_expr_id    *ir_pass2_expr_id_new(ir_pass2 *self, const ir_term_id *id);
ir_expr_lit   *ir_pass2_expr_lit_new(ir_pass2 *self, ir_term_lit *lit);
ir_expr_index *ir_pass2_expr_index_new(ir_pass2 *self, ir_expr_base *indexed,
                                       p_list args);
ir_expr_call  *ir_pass2_expr_call_new(ir_pass2 *self, ir_expr_base *callee,
                                      p_list args);

ir_expr_base *ir_pass2_expr_unary_new(ir_pass2 *self, ir_expr_base *first,
                                      ir_expr_unary_enum op);

ir_expr_base *ir_pass2_expr_binary_new(ir_pass2 *self, ir_expr_base *first,
                                       ir_expr_base       *second,
                                       ir_expr_binary_enum op);

#define IR_PASS2_ANTLR3_USERP()                                                \
  ((ir_pass2 *)ctx->pTreeParser->rec->state->userp)

#define IR_PASS2_CHARS(id)                                                     \
  ((const char *)id->getText(id)->to8(id->getText(id))->chars)
