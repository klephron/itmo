#pragma once

#include <stddef.h>

enum option2_inter_t_enum {
  OPTION2_INTER_T_UNKNOWN = 0,
  OPTION2_INTER_T_BOOL = 1,
  OPTION2_INTER_T_BYTE = 2,
  OPTION2_INTER_T_INT = 3,
  OPTION2_INTER_T_UINT = 4,
  OPTION2_INTER_T_LONG = 5,
  OPTION2_INTER_T_ULONG = 6,
  OPTION2_INTER_T_CHAR = 7,
  OPTION2_INTER_T_STRING = 8,
  OPTION2_INTER_T_VOID = 9,
  OPTION2_INTER_T_ARRAY = 10,
};

struct option2_inter_t_base {
  enum option2_inter_t_enum type;
};

struct option2_inter_t_array {
  struct option2_inter_t_base base;
  size_t dimensions;
  struct option2_inter_t_base *elem_type;
};

typedef struct option2_inter_t_base option2_inter_t_base;
typedef struct option2_inter_t_array option2_inter_t_array;
typedef enum option2_inter_t_enum option2_inter_t_enum;

void option2_inter_t_free(struct option2_inter_t_base *generic);

option2_inter_t_base *option2_inter_t_base_new(option2_inter_t_enum type);
option2_inter_t_array *
option2_inter_t_array_new(size_t dimensions, option2_inter_t_base *elem_type);
