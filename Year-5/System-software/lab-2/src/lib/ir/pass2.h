#pragma once

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

#define IR_PASS2_ANTLR3_USERP()                                                \
  ((ir_pass2 *)ctx->pTreeParser->rec->state->userp)

#define IR_PASS2_CHARS(id)                                                     \
  ((const char *)id->getText(id)->to8(id->getText(id))->chars)
