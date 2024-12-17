#pragma once

#include "compiler/span/span.h"
#include <stddef.h>

typedef struct mir_debug_line_struct {
  const char *source_ref;
  size_t      line;
} mir_debug_line;

mir_debug_line *mir_debug_line_new(const char *source_ref, size_t line);
void            mir_debug_line_free(mir_debug_line *self);
mir_debug_line *mir_debug_line_from_span(const span *span);
