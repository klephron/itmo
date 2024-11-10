#pragma once

#include "util/list.h"

typedef struct hir_struct            hir;
typedef struct hir_subroutine_struct hir_subroutine;

struct hir_struct {
  list name_resolution_table;
  list subroutines;
};

// struct hir_subroutine_struct {
// type
// };
