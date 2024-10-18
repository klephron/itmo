#pragma once

#include "util/container.h"
#include <stddef.h>

struct hashmap {

  size_t (*size)(struct hashmap *self);

  void (*free)(struct hashmap *self);

  container_f_cmp    *f_cmp;
  container_f_new    *f_new;
  container_f_delete *f_delete;
};

typedef struct hashmap hashmap, *p_hashmap;
