#pragma once

#include "span.h"
#include "util/log.h"
#include "util/macro.h"

span span_make(ANTLR3_BASE_TREE *first, ANTLR3_BASE_TREE *last,
               const char *source) {
  if (!first || !last || !first->getToken(first) || !last->getToken(last)) {
    debug("span is set to default");
    return (span){
        .source = source,
        .line_start = 0,
        .line_end = 0,
        .pos_start = 0,
        .pos_end = 0,
    };
  }
  ANTLR3_COMMON_TOKEN *first_token = first->getToken(first);
  ANTLR3_COMMON_TOKEN *last_token = last->getToken(last);

  return (span){
      .source = source,
      .line_start = first_token->line,
      .line_end = last_token->line,
      .pos_start = first_token->getCharPositionInLine(first_token),
      .pos_end = last_token->getCharPositionInLine(last_token) +
                 (last_token->stop - last_token->start + 1),
  };
}

span *span_new(ANTLR3_BASE_TREE *first, ANTLR3_BASE_TREE *last,
               const char *source) {
  span *self = MALLOC(span);
  *self = span_make(first, last, source);
  return self;
}

void span_free(span *self) {
  if (self) {
    free(self);
  }
}

span *span_copy(span *self) {
  if (self) {
    span *new_self = MALLOC(span);
    *new_self = *self;
    return new_self;
  }
  return NULL;
}
