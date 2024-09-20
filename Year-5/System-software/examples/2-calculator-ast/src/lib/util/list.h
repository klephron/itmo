#pragma once

#include <stdint.h>

struct list_data_struct {
  char *key;
  int value;
};

struct list_entry_struct {
  struct list_data_struct data;
  struct list_entry_struct *next;
};

struct result_int_struct {
  uint8_t code;
  int value;
};

struct list_struct {
  struct list_entry_struct *head;

  struct result_int_struct (*get)(struct list_struct *this, const char *key);
  void (*put)(struct list_struct *this, const char *key, const int value);
  void (*free)(struct list_struct *this);
};

typedef struct list_data_struct list_data;
typedef struct list_entry_struct list_entry, *p_list_entry;
typedef p_list_entry list_iter;
typedef struct list_struct list, *p_list;

typedef struct result_int_struct result_int;


p_list list_new();
