#pragma once

#include "mir/mir.h"

typedef struct x86_64_struct {

} x86_64;

x86_64 *x86_64_build(mir *mir);
char   *x86_64_emit(x86_64 *self);
void    x86_64_free(x86_64 *self);
