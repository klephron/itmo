#pragma once

#include <stddef.h>
#define STRING_BUFFER_INITIAL_CAPACITY 256
#define STRING_BUFFER_GROWTH_FACTOR 2

typedef struct strbuf_struct {
  char *data;
  char *cur;

  size_t capacity;
  double growth_factor;
} strbuf;

strbuf *strbuf_new(int capacity, double growth_factor);
void    strbuf_free(strbuf *self);
void    strbuf_append(strbuf *self, const char *data);
char   *strbuf_data(strbuf *self);
char   *strbuf_detach(strbuf *self);
size_t  strbuf_size(strbuf *self);
void    strbuf_reset(strbuf *self);
