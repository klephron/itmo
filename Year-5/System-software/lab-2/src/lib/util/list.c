#include <stdlib.h>
#include <string.h>

#include "util/list.h"
#include "util/macro.h"

static void *list_get(p_list self, const void *data) {
  for (list_iter it = self->head; it != NULL; it = it->next) {
    if (!self->f_cmp(it->data, data)) {
      return it->data;
    }
  }
  return NULL;
}

static void list_replace(struct list_struct *self, list_iter it, void *data) {
  self->f_delete(it->data);
  it->data = self->f_new(data);
}

static void list_put(p_list self, void *data) {
  if (self->unique) {
    for (list_iter it = self->head; it != NULL; it = it->next) {
      if (!self->f_cmp(it->data, data)) {
        list_replace(self, it, data);
        return;
      }
    }
  }

  p_list_entry entry = MALLOC(list_entry);
  entry->data = self->f_new(data);
  entry->next = NULL;

  if (self->tail == NULL) {
    self->head = entry;
  } else {
    self->tail->next = entry;
  }
  self->tail = entry;
}

static size_t list_size(p_list self) {
  size_t size = 0;
  for (list_iter it = self->head; it != NULL; it = it->next) {
    ++size;
  }
  return size;
}

static void list_free(p_list self) {
  if (self != NULL) {
    list_iter next;
    for (list_iter it = self->head; it != NULL; it = next) {
      next = it->next;
      self->f_delete(it->data);
      free(it);
    }
    free(self);
  }
}

p_list list_new(container_f_cmp cmp, container_f_new new,
                container_f_delete delete, int unique) {
  p_list self = MALLOC(list);

  self->unique = unique;

  self->head = NULL;
  self->tail = NULL;
  self->get = list_get;
  self->put = list_put;
  self->replace = list_replace;
  self->free = list_free;
  self->size = list_size;

  self->f_cmp = cmp;
  self->f_new = new;
  self->f_delete = delete;
  return self;
}
