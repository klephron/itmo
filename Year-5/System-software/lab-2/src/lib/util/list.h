#pragma once

#include <stdint.h>
#include <stdio.h>

#include "util/container.h"

struct list_entry_struct {
  void *data;
  struct list_entry_struct *next;
};

typedef struct list_entry_struct list_entry, *list_it;

struct list_struct {
  struct list_entry_struct *head;
  struct list_entry_struct *tail;

  void (*push_back)(struct list_struct *self, void *data);
  list_it (*find)(const struct list_struct *self, const void *data);
  void (*insert)(struct list_struct *self, list_it it, void *data);
  size_t (*size)(const struct list_struct *self);

  void (*free)(struct list_struct *self);

  container_f_cmp *f_cmp;
  container_f_new *f_new;
  container_f_delete *f_delete;
};

typedef struct list_struct list, *p_list;

p_list list_new(container_f_cmp, container_f_new, container_f_delete);
