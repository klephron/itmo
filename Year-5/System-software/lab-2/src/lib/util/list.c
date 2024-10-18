#include <stdlib.h>
#include <string.h>

#include "util/list.h"
#include "util/macro.h"

static void *list_get(p_list this, const void *data) {
  for (list_iter it = this->head; it != NULL; it = it->next) {
    if (!this->f_cmp(it->data, data)) {
      return it->data;
    }
  }
  return NULL;
}

static void list_replace(struct list_struct *this, list_iter it, void *data) {
  this->f_delete(it->data);
  it->data = this->f_new(data);
}

static void list_put(p_list this, void *data) {
  if (this->unique) {
    for (list_iter it = this->head; it != NULL; it = it->next) {
      if (!this->f_cmp(it->data, data)) {
        list_replace(this, it, data);
        return;
      }
    }
  }

  p_list_entry entry = MALLOC(list_entry);
  entry->data = this->f_new(data);
  entry->next = NULL;

  if (this->tail == NULL) {
    this->head = entry;
  } else {
    this->tail->next = entry;
  }
  this->tail = entry;
}

static size_t list_size(p_list this) {
  size_t size = 0;
  for (list_iter it = this->head; it != NULL; it = it->next) {
    ++size;
  }
  return size;
}

static void list_free(p_list this) {
  if (this != NULL) {
    list_iter next;
    for (list_iter it = this->head; it != NULL; it = next) {
      next = it->next;
      this->f_delete(it->data);
      free(it);
    }
    free(this);
  }
}

p_list list_new(container_f_cmp cmp, container_f_new new,
                container_f_delete delete, int unique) {
  p_list this = MALLOC(list);

  this->unique = unique;

  this->head = NULL;
  this->tail = NULL;
  this->get = list_get;
  this->put = list_put;
  this->replace = list_replace;
  this->free = list_free;
  this->size = list_size;

  this->f_cmp = cmp;
  this->f_new = new;
  this->f_delete = delete;
  return this;
}
