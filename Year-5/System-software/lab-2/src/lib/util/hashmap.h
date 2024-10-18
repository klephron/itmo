#pragma once

#include <stddef.h>

struct hashmap_struct {
  void (*free)(struct hashmap_struct *self);
  size_t (*size)(struct hashmap_struct *self);
};

typedef struct hashmap_struct hashmap, *p_hashmap;
