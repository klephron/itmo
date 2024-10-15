#pragma once

#include "util/list.h"

struct option2_inter_userp {
  struct option2_inter *inter;
  const char *file_name;
};

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

struct option2_inter_signature {
  char *name;
  p_list params;
  struct option2_inter_t_base *ret;
};

struct option2_inter_subroutine {
  struct option2_inter_signature *signature;
  char *file_name;
  void *blocks;
  p_list operations; // tree-like structure
};

struct option2_inter {
  p_list subroutines; // option2_inter_subroutine
  p_list exceptions;  // exception

  void (*free)(struct option2_inter *);
};

typedef struct option2_inter_userp option2_inter_userp;
typedef struct option2_inter option2_inter;
typedef struct option2_inter_subroutine option2_inter_subroutine;
typedef struct option2_inter_signature option2_inter_signature;
typedef struct option2_inter_t_base option2_inter_t_base;
typedef struct option2_inter_t_array option2_inter_t_array;
typedef enum option2_inter_t_enum option2_inter_t_enum;

option2_inter_t_base *option2_inter_t_base_new(option2_inter_t_enum type);
option2_inter_t_array *
option2_inter_t_array_new(size_t dimensions, option2_inter_t_base *elem_type);
option2_inter_signature *option2_inter_signature_new(char *name, p_list params,
                                                     option2_inter_t_base *ret);
option2_inter_subroutine *
option2_inter_subroutine_new(const option2_inter_userp *userp,
                             option2_inter_signature *signature, void *blocks,
                             p_list operations);
option2_inter *option2_inter_new();
