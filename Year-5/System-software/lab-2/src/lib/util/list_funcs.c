#include "util/list_funcs.h"

#include "util/macro.h"

#include <stdlib.h>
#include <string.h>

// STRING_STRING
int list_string_string_cmp(const void *_lsv, const void *_rsv) {
  const string_string *lsv = _lsv;
  const string_string *rsv = _rsv;
  return strcmp(lsv->key, rsv->key);
}

void *list_string_string_new(void *_data) {
  string_string *data = _data;

  string_string *new_data = MALLOC(string_string);
  new_data->key = strdup(data->key);
  new_data->value = strdup(data->value);

  return new_data;
}

void list_string_string_delete(void *_data) {
  string_string *data = _data;
  free(data->key);
  free(data->value);
  free(data);
}

// CHARS
int list_chars_cmp(const void *_lsv, const void *_rsv) {
  const char *lsv = _lsv;
  const char *rsv = _rsv;
  return strcmp(lsv, rsv);
}

void *list_chars_new(void *_data) { return strdup(_data); }

void list_chars_delete(void *_data) { free(_data); }

// FALSE
int list_false_cmp(const void *lsv, const void *rsv) {
  UNUSED(lsv);
  UNUSED(rsv);
  return 1;
}

// MOVE
void *list_move_new(void *data) { return data; }
