#include "util/list.h"

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
  OPTION2_INTER_T_IDENTIFIER = 11
};

struct option2_inter_t {
  enum option2_inter_t_enum type;
  void *sub;
};

struct option2_inter_signature {
  char *name;
  p_list params;
  struct option2_inter_t ret;
};

struct option2_inter_subroutine {
  struct option2_inter_signature signature;
  char *file_name;
  void *blocks;
  p_list operations;
};

struct option2_inter {
  p_list subroutines;
  p_list exceptions;

  void (*free)(struct option2_inter *);
};

struct option2_inter *option2_inter_build(const p_list asts);

typedef struct option2_inter option2_inter;
typedef struct option2_inter_subroutine option2_inter_subroutine;
typedef struct option2_inter_signature option2_inter_signature;
typedef struct option2_inter_t option2_inter_t;
typedef enum option2_inter_t_enum option2_inter_t_enum;

