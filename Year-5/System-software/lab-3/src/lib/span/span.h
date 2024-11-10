#pragma once

#include <antlr3basetree.h>
#include <antlr3commontoken.h>

typedef struct span_struct {
  const char *source;
  size_t      line_start;
  size_t      line_end;
  size_t      pos_start;
  size_t      pos_end;
} span;

span  span_make(ANTLR3_BASE_TREE *first, ANTLR3_BASE_TREE *last,
                const char *source);
span *span_new(ANTLR3_BASE_TREE *first, ANTLR3_BASE_TREE *last,
               const char *source);
void  span_free(span *self);
span *span_copy(span *self);
