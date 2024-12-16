#pragma once

#include <stddef.h>
#define STRING_BUFFER_INITIAL_CAPACITY 256
#define STRING_BUFFER_GROWTH_FACTOR 2

typedef struct string_buffer_struct {
  char *data;
  char *cur;

  size_t capacity;
  double growth_factor;
} string_buffer;

string_buffer *string_buffer_new(int capacity, double growth_factor);
void           string_buffer_free(string_buffer *self);
void           string_buffer_append(string_buffer *self, const char *data);
char          *string_buffer_data(string_buffer *self);
char          *string_buffer_detach(string_buffer *self);
size_t         string_buffer_size(string_buffer *self);
void           string_buffer_reset(string_buffer *self);
