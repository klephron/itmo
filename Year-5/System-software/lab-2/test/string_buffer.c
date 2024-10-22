#include <criterion/criterion.h>

#include "util/string_buffer.h"

Test(string_buffer, test1) {
  string_buffer *buffer = string_buffer_new(4,0);

  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");

  string_buffer_free(buffer);
}

Test(string_buffer, test2) {
  string_buffer *buffer = string_buffer_new(4,0);

  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");
  string_buffer_append(buffer,"hello, world!");

  char *data = string_buffer_detach(buffer);
  free(data);
}
