#include <stdlib.h>
#include <string.h>

#include "util/list.h"
#include "util/macro.h"

p_list list_new(container_f_cmp cmp, container_f_new new,
                container_f_delete delete) {
  p_list self = MALLOC(list);

  self->head = NULL;
  self->tail = NULL;

  self->f_cmp = cmp;
  self->f_new = new;
  self->f_delete = delete;

  return self;
}

void list_free(p_list self) {
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

list_it list_find(const struct list *self, const void *data) {
  for (list_it it = self->head; it; it = it->next) {
    if (!self->f_cmp(it->data, data)) {
      return it;
    }
  }
  return NULL;
}

void list_push_back(p_list self, void *data) {
  list_node *node = MALLOC(list_node);
  node->data = self->f_new(data);
  node->next = NULL;

  if (self->tail) {
    self->tail->next = node;
  } else {
    self->head = node;
  }
  self->tail = node;
}

void list_push_front(p_list self, void *data) {
  list_node *node = MALLOC(list_node);
  node->data = self->f_new(data);
  node->next = self->head;
  self->head = node;
  if (!self->tail) {
    self->tail = node;
  }
}

void *list_front(p_list self) { return self->head ? self->head->data : NULL; }

void *list_back(p_list self) { return self->tail ? self->tail->data : NULL; }

// won't free data
void *list_pop_front(p_list self) {
  list_node *node = self->head;
  self->head = node->next;
  if (!self->head) {
    self->tail = NULL;
  }
  void *data = node->data;
  free(node);
  return data;
}

void list_insert(struct list *self, list_it it, void *data) {
  if (it) {
    self->f_delete(it->data);
    it->data = self->f_new(data);
  } else {
    list_push_back(self, data);
  }
}

size_t list_size(const struct list *self) {
  size_t size = 0;
  for (list_it it = self->head; it; it = it->next) {
    ++size;
  }
  return size;
}
