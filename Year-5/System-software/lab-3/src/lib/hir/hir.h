#pragma once

#include "hir/symbols.h"

typedef struct hir_subroutine {
} hir_subroutine;

hir_subroutine *hir_subroutine_new();
void            hir_subroutine_free(hir_subroutine *self);

static inline void container_delete_hir_subroutine(void *data) {
  hir_subroutine_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_hir_subroutine, hir_subroutine,
                           container_cmp_false, container_new_move,
                           container_delete_hir_subroutine);

typedef struct hir_struct {
  list_hir_symbol     *symbols;
  list_hir_subroutine *subroutines;
} hir;

hir *hir_new();
void hir_free(hir *self);
