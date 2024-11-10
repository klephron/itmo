#pragma once

// #include "ir/ctx.h"
// #include "ir/pass.h"
// #include "ir/subroutine.h"

// struct ir_pass1 {
//   struct ir_pass_base base;
//   struct ir_ctx      *ctx;
//   const char         *file_name;
// };

// typedef struct ir_pass1 ir_pass1;

// void ir_pass1_init(ir_pass1 *self, struct ir_ctx *ctx);
// void ir_pass1_deinit(ir_pass1 *self);

// ir_t_base *ir_pass1_t_bool_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_byte_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_int_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_uint_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_long_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_ulong_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_char_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_string_new(ir_pass1 *self);
// ir_t_base *ir_pass1_t_custom_new(ir_pass1 *self, const char *name);
// ir_t_base *ir_pass1_t_array_new(ir_pass1 *self, size_t dimension,
//                                 ir_t_base *type);

// ir_var *ir_pass1_param_new(ir_pass1 *self, ir_term_id *id, ir_t_base *type);

// p_list ir_pass1_params_new(ir_pass1 *self);
// int    ir_pass1_param_add(ir_pass1 *self, p_list list, ir_var *param);

// ir_signature *ir_pass1_signature_new(ir_pass1 *self, ir_term_id *id,
//                                      p_list params, ir_t_base *ret);

// ir_subroutine *ir_pass1_subroutine_new(const ir_pass1 *self,
//                                        ir_signature   *signature);
// int ir_pass1_subroutine_add(ir_pass1 *self, ir_subroutine *subroutine);

// #define IR_PASS1_ANTLR3_USERP()                                                \
//   ((ir_pass1 *)ctx->pTreeParser->rec->state->userp)
