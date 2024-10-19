#include "util/container_util.h"

#include "util/macro.h"

#include <stdlib.h>
#include <string.h>

// CMP
int container_cmp_chars_chars(const void *_lsv, const void *_rsv) {
  const chars_chars *lsv = _lsv;
  const chars_chars *rsv = _rsv;
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
void *container_new_chars_chars(void *_data) {
  chars_chars *data = _data;

  chars_chars *new_data = MALLOC(chars_chars);
  new_data->key = strdup(data->key);
  new_data->value = strdup(data->value);

  return new_data;
}

void *container_new_chars(void *_data) { return strdup(_data); }

void *container_new_move(void *data) { return data; }

// DELETE
void container_delete_chars_chars(void *_data) {
  chars_chars *data = _data;
  free(data->key);
  free(data->value);
  free(data);
}

void container_delete_chars(void *_data) { free(_data); }

// HASH
size_t container_hash_chars(void *_data) {
  const size_t pow = 757;
  size_t       hash = 353;

  char *kc = _data;
  while (*kc != '\0') {
    hash = hash * pow + *kc++;
  }

  return hash;
}

size_t container_hash_chars_chars(void *_data) {
  chars_chars *data = _data;
  return container_hash_chars(data->key);
}
