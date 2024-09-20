#include <stdlib.h>
#include <string.h>


#include "util/list.h"

int list_has_next(list_iter this) {
  if (this->next != NULL) {
    return 1;
  }
  return 0;
}

result_int list_get(p_list this, const char *key) {
  for (list_iter it = this->head; it != NULL; it = it->next) {
    if (strcmp(it->data.key, key) == 0) {
      return (result_int){.code = 0, .value = it->data.value};
    }
  }
  return (result_int){.code = -1, .value = 0};
}

void list_put(p_list this, const char *key, const int value) {
  for (list_iter it = this->head; it != NULL; it = it->next) {
    if (strcmp(it->data.key, key) == 0) {
      it->data.value = value;
      return;
    }
  }

  list_iter it = this->head;

  if (it != NULL) {
    while (list_has_next(it)) {
      it = it->next;
    }
  }

  p_list_entry tail = malloc(sizeof(list_entry));
  tail->data.key = strdup(key);
  tail->data.value = value;
  tail->next = NULL;

  if (it == NULL) {
    this->head = tail;
  } else {
    it->next = tail;
  }
}

void list_free(p_list this) {
  if (this != NULL) {
    list_iter next;
    for (list_iter it = this->head; it != NULL; it = next) {
      next = it->next;
      free(it->data.key);
      free(it);
    }
    free(this);
  }
}

p_list list_new() {
  p_list this = malloc(sizeof(list));
  this->head = NULL;
  this->get = list_get;
  this->put = list_put;
  this->free = list_free;
  return this;
}
