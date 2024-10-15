#pragma once

#include "option2_inter/ctx.h"

struct option2_inter_userp {
  struct option2_inter *inter;
  const char *file_name;
};

typedef struct option2_inter_userp option2_inter_userp;
