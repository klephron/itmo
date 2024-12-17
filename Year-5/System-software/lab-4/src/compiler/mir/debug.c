#include "debug.h"

#include "util/macro.h"
#include <stdlib.h>

mir_debug_line *mir_debug_line_new(const char *source_ref, size_t line) {
  mir_debug_line *self = MALLOC(mir_debug_line);
  self->source_ref = source_ref;
  self->line = line;
  return self;
}

void mir_debug_line_free(mir_debug_line *self) {
  if (self) {
    free(self);
  }
}

mir_debug_line *mir_debug_line_from_span(const span *span) {
  if (span) {
    return mir_debug_line_new(span->source_ref, span->line_start);
  } else {
    return NULL;
  }
}
