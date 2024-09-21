#include "util/list_funcs.h"

#include <stdlib.h>
#include <string.h>

int string_equals(const void *lsv, const void *rsv) { return strcmp(lsv, rsv); }

list_data *string_string_new(const void *_lsv, const  void *_rsv) {
  const char *lsv = _lsv;
  const char *rsv = _rsv;

  list_data *data = malloc(sizeof(list_data));
  data->key = strdup(lsv);
  data->value = strdup(rsv);

  return data;
}

void string_string_delete(list_data *data) {
  free(data->key);
  free(data->value);
  free(data);
}
