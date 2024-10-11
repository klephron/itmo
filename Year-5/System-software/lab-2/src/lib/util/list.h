#pragma once

#include <stdint.h>
#include <stdio.h>

struct list_entry_struct {
  void *data;
  struct list_entry_struct *next;
};

typedef int list_func_equals(const void *lsv, const void *rsv);
typedef void *list_func_new(void *data);
typedef void list_func_delete(void *data);

struct list_struct {
  struct list_entry_struct *head;
  int unique;

  void *(*get)(struct list_struct *this, const void *data);
  void (*put)(struct list_struct *this, void *data);
  void (*free)(struct list_struct *this);
  size_t (*size)(struct list_struct *this);

  list_func_equals *f_equals;
  list_func_new *f_new;
  list_func_delete *f_delete;
};

typedef struct list_entry_struct list_entry, *p_list_entry;
typedef p_list_entry list_iter;
typedef struct list_struct list, *p_list;

p_list list_new(list_func_equals, list_func_new, list_func_delete, int unique);
