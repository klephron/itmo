#include "util/container_funcs.h"

#include "util/macro.h"

#include <stdlib.h>
#include <string.h>

// CMP
int container_cmp_string_string(const void *_lsv, const void *_rsv) {
  const string_string *lsv = _lsv;
  const string_string *rsv = _rsv;
  return strcmp(lsv->key, rsv->key);
}

int container_cmp_chars(const void *_lsv, const void *_rsv) {
  const char *lsv = _lsv;
  const char *rsv = _rsv;
  return strcmp(lsv, rsv);
}

int container_cmp_false(const void *lsv, const void *rsv) {
  UNUSED(lsv);
  UNUSED(rsv);
  return 1;
}

// NEW
void *container_new_string_string(void *_data) {
  string_string *data = _data;

  string_string *new_data = MALLOC(string_string);
  new_data->key = strdup(data->key);
  new_data->value = strdup(data->value);

  return new_data;
}

void *container_new_chars(void *_data) { return strdup(_data); }

void *container_new_move(void *data) { return data; }

// DELETE
void container_delete_string_string(void *_data) {
  string_string *data = _data;
  free(data->key);
  free(data->value);
  free(data);
}

void container_delete_chars(void *_data) { free(_data); }
