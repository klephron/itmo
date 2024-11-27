#pragma once

#include "compiler/mir/mir.h"

typedef enum cg_x86_64_regs {
  CG_X86_64_REGS_UNKNOWN,
} cg_x86_64_regs;

typedef struct cg_x86_64_struct {

} cg_x86_64;

cg_x86_64 *cg_x86_64_build(mir *mir);
char      *cg_x86_64_emit(cg_x86_64 *self);
void       cg_x86_64_free(cg_x86_64 *self);
