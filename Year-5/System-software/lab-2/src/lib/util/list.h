#pragma once

#include <stdint.h>
#include <stdio.h>

struct list_entry_struct {
  void *data;
  struct list_entry_struct *next;
};

typedef int list_func_cmp(const void *lsv, const void *rsv);
typedef void *list_func_new(void *data);
typedef void list_func_delete(void *data);

typedef struct list_entry_struct list_entry, *p_list_entry;
typedef p_list_entry list_iter;

struct list_struct {
  struct list_entry_struct *head;
  struct list_entry_struct *tail;
  int unique;

  void *(*get)(struct list_struct *this, const void *data);
  void (*put)(struct list_struct *this, void *data);
  void (*free)(struct list_struct *this);
  void (*replace)(struct list_struct *this, list_iter it, void *data);
  size_t (*size)(struct list_struct *this);

  list_func_cmp *f_cmp;
  list_func_new *f_new;
  list_func_delete *f_delete;
};

typedef struct list_struct list, *p_list;

p_list list_new(list_func_cmp, list_func_new, list_func_delete, int unique);
