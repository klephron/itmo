// #pragma once

// #include "ir/loc.h"
// #include "ir/type.h"

// enum ir_term_enum { IR_TERM_UNKNOWN, IR_TERM_LITERAL, IR_TERM_IDENTIFIER };

// struct ir_term_base {
//   enum ir_term_enum kind;
//   ir_loc            loc;
// };

// union ir_term_lit_v {
//   long  v_int;
//   char *v_str;
//   char  v_char;
//   int   v_bool;
// };

// struct ir_term_lit {
//   struct ir_term_base base;
//   enum ir_t_enum      type;
//   union ir_term_lit_v u;
// };

// struct ir_term_id {
//   struct ir_term_base base;
//   char               *name;
// };

// typedef enum ir_term_enum   ir_term_enum;
// typedef struct ir_term_base ir_term_base;
// typedef union ir_term_lit_v ir_term_lit_v;
// typedef struct ir_term_lit  ir_term_lit;
// typedef struct ir_term_id   ir_term_id;

// void         ir_term_lit_free(ir_term_lit *self);
// ir_term_lit *ir_term_lit_new(ir_loc loc, ir_t_enum type, ir_term_lit_v v);

// void        ir_term_id_free(ir_term_id *self);
// ir_term_id *ir_term_id_new(ir_loc loc, const char *);
// int         ir_term_id_equals(const ir_term_id *lsv, const ir_term_id *rsv);

// void          ir_term_free(struct ir_term_base *generic);
// ir_term_base *ir_term_copy(struct ir_term_base *generic);
