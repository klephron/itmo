#include <stdlib.h>
#include <string.h>

#include "util/list.h"

int list_has_next(list_iter this) {
  if (this->next != NULL) {
    return 1;
  }
  return 0;
}

void *list_get(p_list this, const void *key) {
  for (list_iter it = this->head; it != NULL; it = it->next) {
    if (this->equals(it->data->key, key)) {
      return it->data->value;
    }
  }
  return NULL;
}

// NOTE: need to recreate object because of memory
void list_put(p_list this, const void *key, const void *value) {
  if (this->unique) {
    for (list_iter it = this->head; it != NULL; it = it->next) {
      if (!this->equals(it->data->key, key)) {
        this->delete (it->data);
        it->data = this->new (key, value);
        return;
      }
    }
  }

  list_iter it = this->head;

  if (it != NULL) {
    while (list_has_next(it)) {
      it = it->next;
    }
  }

  p_list_entry tail = malloc(sizeof(list_entry));
  tail->data = this->new (key, value);
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
      this->delete (it->data);
      free(it);
    }
    free(this);
  }
}

p_list list_new(int (*equals)(const void *lsv_key, const void *rsv_key),
                list_data *(*new)(const void *key, const void *value),
                void (*delete)(list_data *data), int unique) {
  p_list this = malloc(sizeof(list));

  this->unique = unique;

  this->head = NULL;
  this->get = list_get;
  this->put = list_put;
  this->free = list_free;

  this->equals = equals;
  this->new = new;
  this->delete = delete;
  return this;
}
