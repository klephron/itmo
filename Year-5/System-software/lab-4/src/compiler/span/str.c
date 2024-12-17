#include "str.h"
#include "util/macro.h"
#include "util/string_buffer.h"
#include <stdio.h>

char *span_str(const span *span) {
  char           buf[32];
  string_buffer *buffer = string_buffer_new(32, 0);

  string_buffer_append(buffer, span->source_ref);
  string_buffer_append(buffer, " ");
  snprintf(buf, STRMAXLEN(buf), "%zu:%zu", span->line_start, span->pos_start);
  string_buffer_append(buffer, buf);
  string_buffer_append(buffer, " ");
  snprintf(buf, STRMAXLEN(buf), "%zu:%zu", span->line_end, span->pos_end);
  string_buffer_append(buffer, buf);

  return string_buffer_detach(buffer);
}
