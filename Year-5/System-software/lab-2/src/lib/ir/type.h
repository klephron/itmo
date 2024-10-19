#pragma once

#include <stddef.h>

enum ir_t_enum {
  IR_T_UNKNOWN,
  IR_T_BOOL,
  IR_T_BYTE,
  IR_T_INT,
  IR_T_UINT,
  IR_T_LONG,
  IR_T_ULONG,
  IR_T_CHAR,
  IR_T_STRING,
  IR_T_VOID,
  IR_T_ARRAY,
  IR_T_CUSTOM,
};

struct ir_t_base {
  enum ir_t_enum type;
};

struct ir_t_array {
  struct ir_t_base  base;
  size_t            dimensions;
  struct ir_t_base *elem_type;
};

struct ir_t_custom {
  struct ir_t_base base;
  char            *name;
};

typedef enum ir_t_enum     ir_t_enum;
typedef struct ir_t_base   ir_t_base;
typedef struct ir_t_array  ir_t_array;
typedef struct ir_t_custom ir_t_custom;

ir_t_base   *ir_t_base_new(ir_t_enum type);
ir_t_array  *ir_t_array_new(size_t dimensions, ir_t_base *elem_type);
ir_t_custom *ir_t_custom_new(const char *name);

void ir_t_free(struct ir_t_base *generic);
int  ir_t_equals(ir_t_base *lsv, ir_t_base *rsv);

const char *ir_t_str(ir_t_base *type);
const char *ir_ttstr(ir_t_enum type);
