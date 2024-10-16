#pragma once

#include <stddef.h>

enum ir_t_enum {
  IR_T_UNKNOWN = 0,
  IR_T_BOOL = 1,
  IR_T_BYTE = 2,
  IR_T_INT = 3,
  IR_T_UINT = 4,
  IR_T_LONG = 5,
  IR_T_ULONG = 6,
  IR_T_CHAR = 7,
  IR_T_STRING = 8,
  IR_T_VOID = 9,
  IR_T_ARRAY = 10,
  IR_T_CUSTOM = 11,
};

struct ir_t_base {
  enum ir_t_enum type;
};

struct ir_t_array {
  struct ir_t_base base;
  size_t dimensions;
  struct ir_t_base *elem_type;
};

struct ir_t_custom {
  struct ir_t_base base;
  char *name;
};

typedef enum ir_t_enum ir_t_enum;
typedef struct ir_t_base ir_t_base;
typedef struct ir_t_array ir_t_array;
typedef struct ir_t_custom ir_t_custom;

void ir_t_free(struct ir_t_base *generic);

ir_t_base *ir_t_base_new(ir_t_enum type);
ir_t_array *ir_t_array_new(size_t dimensions, ir_t_base *elem_type);
ir_t_custom *ir_t_custom_new(char *name); // move

int ir_t_equals(ir_t_base *lsv, ir_t_base *rsv);
