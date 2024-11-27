#include "util/string_buffer.h"
#include "util/log.h"
#include "util/macro.h"
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

void string_buffer_free(string_buffer *buffer) {
  if (buffer) {
    free(buffer->data);
    free(buffer);
  }
}

string_buffer *string_buffer_new(int capacity, double growth_factor) {
  string_buffer *self = MALLOC(string_buffer);
  if (capacity > 0) {
    self->capacity = capacity;
  } else {
    self->capacity = STRING_BUFFER_INITIAL_CAPACITY;
  }
  self->data = malloc(self->capacity);
  self->data[0] = '\0';
  self->cur = self->data;
  if (growth_factor > 1) {
    self->growth_factor = growth_factor;
  } else {
    self->growth_factor = STRING_BUFFER_GROWTH_FACTOR;
  }
  return self;
}

static void string_buffer_resize(string_buffer *self) {
  size_t new_capacity = self->capacity * self->growth_factor;
  size_t size = string_buffer_size(self);

  if (new_capacity < self->capacity) {
    error("string_buffer new_capacity is less than previous");
  }

  self->data = realloc(self->data, new_capacity);
  self->cur = self->data + (size / sizeof(char));
  self->capacity = new_capacity;
}

void string_buffer_append(string_buffer *self, const char *data) {
  size_t data_size = strlen(data) * sizeof(char);
  size_t size = string_buffer_size(self);

  while (size + data_size >= self->capacity) {
    string_buffer_resize(self);
  }

  memcpy(self->cur, data, data_size + 1); // copy with null terminator
  self->cur += (data_size / sizeof(char));
}

char *string_buffer_data(string_buffer *self) { return self->data; }

char *string_buffer_detach(string_buffer *self) {
  char *data = NULL;
  if (self) {
    data = string_buffer_data(self);
    free(self);
  }
  return data;
}

size_t string_buffer_size(string_buffer *self) {
  return (((intptr_t)self->cur) - ((intptr_t)self->data));
}
