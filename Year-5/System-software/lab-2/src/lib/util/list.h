#pragma once

#include <stdint.h>
#include <stdio.h>

#include "util/container.h"

struct list_entry_struct {
  void *data;
  struct list_entry_struct *next;
};

typedef struct list_entry_struct list_entry, *p_list_entry;
typedef p_list_entry list_iter;

struct list_struct {
  struct list_entry_struct *head;
  struct list_entry_struct *tail;
  int unique;

  void *(*get)(struct list_struct *self, const void *data);
  void (*put)(struct list_struct *self, void *data);
  void (*free)(struct list_struct *self);
  void (*replace)(struct list_struct *self, list_iter it, void *data);
  size_t (*size)(struct list_struct *self);

  container_f_cmp *f_cmp;
  container_f_new *f_new;
  container_f_delete *f_delete;
};

typedef struct list_struct list, *p_list;

p_list list_new(container_f_cmp, container_f_new, container_f_delete,
                int unique);
