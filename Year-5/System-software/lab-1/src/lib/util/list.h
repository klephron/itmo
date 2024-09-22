#pragma once

#include <stdint.h>
#include <stdio.h>

struct list_data_struct {
  void *key;
  void *value;
};

struct list_entry_struct {
  struct list_data_struct *data;
  struct list_entry_struct *next;
};

struct list_struct {
  struct list_entry_struct *head;
  int unique;

  void *(*get)(struct list_struct *this, const void *key);
  void (*put)(struct list_struct *this, const void *key, const void *value);
  void (*free)(struct list_struct *this);
  size_t (*size)(struct list_struct *this);

  // NOTE: for true OOP may be present in struct instead
  int (*equals)(const void *lsv_key, const void *rsv_key);
  struct list_data_struct *(*new)(const void *key, const void *value);
  void (*delete)(struct list_data_struct *data);
};

typedef struct list_data_struct list_data;
typedef struct list_entry_struct list_entry, *p_list_entry;
typedef p_list_entry list_iter;
typedef struct list_struct list, *p_list;

p_list list_new(int (*equals)(const void *lsv_key, const void *rsv_key),
                list_data *(*new)(const void *key, const void *value),
                void (*delete)(list_data *data), int unique);
