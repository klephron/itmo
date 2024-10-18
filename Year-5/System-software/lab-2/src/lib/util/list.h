#pragma once

#include "util/container.h"
#include <stddef.h>

struct list_node {
  void             *data;
  struct list_node *next;
};

typedef struct list_node list_node, *list_it;

struct list {
  struct list_node *head;
  struct list_node *tail;

  container_f_cmp    *f_cmp;
  container_f_new    *f_new;
  container_f_delete *f_delete;
};

typedef struct list list, *p_list;

p_list  list_new(container_f_cmp, container_f_new, container_f_delete);
void    list_free(list *self);
void    list_push_back(list *self, void *data);
list_it list_find(const list *self, const void *data);
void    list_insert(list *self, list_it it, void *data);
size_t  list_size(const list *self);
