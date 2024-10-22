#pragma once

#include "ir/expr.h"
#include "ir/function.h"
#include "ir/pass.h"
#include "ir/scope.h"
#include "ir/terminal.h"

struct ir_pass2 {
  struct ir_pass_base base;
  struct ir_ctx      *ctx;
  const char         *file_name;
  scope_stack         scope_stack;
  p_list              break_stack;
  ir_block_base      *graph;
};

typedef struct ir_pass2 ir_pass2;

void ir_pass2_init(ir_pass2 *self, struct ir_ctx *ctx, const char *file_name);
void ir_pass2_deinit(ir_pass2 *self);

ir_term_lit *ir_pass2_str(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_rune(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_hex(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_bits(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_dec(ir_pass2 *self, const char *str);
ir_term_lit *ir_pass2_bool(ir_pass2 *self, const char *str);

ir_term_id *ir_pass2_term_id_new(ir_pass2 *self, const char *id);

ir_expr_base *ir_pass2_expr_id_new(ir_pass2 *self, const ir_term_id *id);
ir_expr_base *ir_pass2_expr_lit_new(ir_pass2 *self, ir_term_lit *lit);
ir_expr_base *ir_pass2_expr_index_new(ir_pass2 *self, ir_expr_base *indexed,
                                      p_list args);
ir_expr_base *ir_pass2_expr_call_new(ir_pass2 *self, ir_expr_base *callee,
                                     p_list args);
ir_expr_base *ir_pass2_expr_unary_new(ir_pass2 *self, ir_expr_base *first,
                                      ir_expr_unary_enum op);
ir_expr_base *ir_pass2_expr_binary_new(ir_pass2 *self, ir_expr_base *first,
                                       ir_expr_base       *second,
                                       ir_expr_binary_enum op);

ir_t_base *ir_pass2_t_bool_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_byte_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_int_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_uint_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_long_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_ulong_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_char_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_string_new(ir_pass2 *self);
ir_t_base *ir_pass2_t_custom_new(ir_pass2 *self, ir_term_id *id);
ir_t_base *ir_pass2_t_array_new(ir_pass2 *self, size_t dimension,
                                ir_t_base *type);

ir_var *ir_pass2_param_new(ir_pass2 *self, ir_term_id *id, ir_t_base *type);

p_list ir_pass2_params_new(ir_pass2 *self);
int    ir_pass2_param_add(ir_pass2 *self, p_list list, ir_var *param);

ir_signature *ir_pass2_signature_new(ir_pass2 *self, ir_term_id *id,
                                     p_list params, ir_t_base *ret);

p_list ir_pass2_ids_new(ir_pass2 *self);
void   ir_pass2_id_add(ir_pass2 *self, p_list list, ir_term_id *id);

p_list ir_pass2_var_new(ir_pass2 *self, p_list ids, ir_t_base *type);

p_hashset ir_pass2_vars_new(ir_pass2 *self);
int       ir_pass2_vars_add(ir_pass2 *self, p_hashset hashset, p_list vars);

ir_subroutine *ir_pass2_subroutine_new(const ir_pass2 *self,
                                       ir_signature *signature, p_hashset vars);
int ir_pass2_subroutine_add(ir_pass2 *self, ir_subroutine *subroutine);

p_list ir_pass2_scope_list_new(ir_pass2 *self);
void   ir_pass2_scope_add(ir_pass2 *self, p_list list, ir_scope *scope);

void ir_pass2_body_before(ir_pass2 *self);
void ir_pass2_body_build(ir_pass2 *self, ir_scope *child);

void      ir_pass2_block_before(ir_pass2 *self);
ir_scope *ir_pass2_block_build(ir_pass2 *self, p_list scopes);
void      ir_pass2_if_before(ir_pass2 *self);
ir_scope *ir_pass2_if_build(ir_pass2 *self, ir_expr_base *expr,
                            ir_scope *s_then, ir_scope *s_else);
void      ir_pass2_while_before(ir_pass2 *self);
ir_scope *ir_pass2_while_build(ir_pass2 *self);
void      ir_pass2_do_before(ir_pass2 *self);
ir_scope *ir_pass2_do_build(ir_pass2 *self);
void      ir_pass2_break_before(ir_pass2 *self);
ir_scope *ir_pass2_break_build(ir_pass2 *self);
void      ir_pass2_expression_before(ir_pass2 *self);
ir_scope *ir_pass2_expression_build(ir_pass2 *self, ir_expr_base *expr);

#define IR_PASS2_ANTLR3_USERP()                                                \
  ((ir_pass2 *)ctx->pTreeParser->rec->state->userp)

#define IR_PASS2_CHARS(id)                                                     \
  ((const char *)id->getText(id)->to8(id->getText(id))->chars)

#define IR_PASS2_SIZE(id) ((size_t)id->children->size(id->children))
