#pragma once

#include "ir/types.h"

enum ir_term_enum {
  IR_TERM_UNKNOWN = 0,
  IR_TERM_LITERAL = 1,
  IR_TERM_IDENTIFIER = 2
};

struct ir_term_base {
  enum ir_term_enum kind;
};

struct ir_term_lit {
  struct ir_term_base base;
  enum ir_t_enum type;
  union value {
    int v_int;
    char *v_str;
    char v_char;
    int v_bool;
  } u;
};

struct ir_term_id {
  struct ir_term_base base;
  char *name;
};

typedef enum ir_term_enum ir_term_enum;
typedef struct ir_term_base ir_term_base;
typedef struct ir_term_lit ir_term_lit;
typedef struct ir_term_id ir_term_id;

void ir_term_lit_free(ir_term_lit *lit);

void ir_term_id_free(ir_term_id *id);
ir_term_id *ir_term_id_new(const char *);
int ir_term_id_equals(const ir_term_id *lsv, const ir_term_id *rsv);

void ir_term_free(struct ir_term_base *generic);
