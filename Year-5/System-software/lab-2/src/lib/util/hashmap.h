#pragma once

#include <stddef.h>

struct hashmap_struct {

  void *(*get)(struct hashmap_struct *self, const void *key);
  void (*put)(struct hashmap_struct *self, void *key, void *data);
  size_t (*size)(struct hashmap_struct *self);

  void (*free)(struct hashmap_struct *self);
};

typedef struct hashmap_struct hashmap, *p_hashmap;
