#include <stdlib.h>
#include <string.h>

#include "util/list.h"
#include "util/macro.h"

static list_it list_find(struct list_struct *self, const void *data) {
  for (list_it it = self->head; it; it = it->next) {
    if (!self->f_cmp(it->data, data)) {
      return it;
    }
  }
  return NULL;
}

static void list_insert(struct list_struct *self, list_it it, void *data) {
  self->f_delete(it->data);
  it->data = self->f_new(data);
}

static void *list_get(p_list self, const void *data) {
  list_it it = list_find(self, data);
  return it ? it->data : NULL;
}

static void list_put(p_list self, void *data) {
  if (self->unique) {
    list_it it = list_find(self, data);
    if (it) {
      list_insert(self, it, data);
      return;
    }
  }

  list_entry *entry = MALLOC(list_entry);
  entry->data = self->f_new(data);
  entry->next = NULL;

  if (self->tail) {
    self->tail->next = entry;
  } else {
    self->head = entry;
  }
  self->tail = entry;
}

static size_t list_size(p_list self) {
  size_t size = 0;
  for (list_it it = self->head; it; it = it->next) {
    ++size;
  }
  return size;
}

static void list_free(p_list self) {
  if (self) {
    list_it next;
    for (list_it it = self->head; it; it = next) {
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
  self->insert = list_insert;
  self->find = list_find;
  self->size = list_size;

  self->free = list_free;

  self->f_cmp = cmp;
  self->f_new = new;
  self->f_delete = delete;
  return self;
}
