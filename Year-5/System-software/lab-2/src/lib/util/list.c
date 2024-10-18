#include <stdlib.h>
#include <string.h>

#include "util/list.h"
#include "util/macro.h"

static list_it list_find(const struct list_struct *self, const void *data) {
  for (list_it it = self->head; it; it = it->next) {
    if (!self->f_cmp(it->data, data)) {
      return it;
    }
  }
  return NULL;
}

static void list_push_back(p_list self, void *data) {
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

static void list_insert(struct list_struct *self, list_it it, void *data) {
  if (it) {
    self->f_delete(it->data);
    it->data = self->f_new(data);
  } else {
    list_push_back(self, data);
  }
}

static size_t list_size(const struct list_struct *self) {
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
                container_f_delete delete) {
  p_list self = MALLOC(list);

  self->head = NULL;
  self->tail = NULL;
  self->push_back = list_push_back;
  self->find = list_find;
  self->insert = list_insert;
  self->size = list_size;

  self->free = list_free;

  self->f_cmp = cmp;
  self->f_new = new;
  self->f_delete = delete;
  return self;
}
