#pragma once

enum ir_pass_enum { IR_PASS_1 = 1, IR_PASS_2 = 2 };

struct ir_pass_base {
  enum ir_pass_enum kind;
};
