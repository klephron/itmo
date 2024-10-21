#pragma once

#include "util/container.h"
#include <stddef.h>

#define HASHTABLE_LOAD_FACTOR 0.75
#define HASHTABLE_GROWTH_FACTOR 2
#define HASHTABLE_INITIAL_CAPACITY 16

struct hashset_list_node {
  void                     *data;
  struct hashset_list_node *next;
};

struct hashset_bucket {
  struct hashset_list_node *head;
};

struct hashset {
  struct hashset_bucket *buckets;

  double load_factor;
  double growth_factor;

  size_t capacity;
  size_t size;

  container_f_cmp    *f_cmp;
  container_f_new    *f_new;
  container_f_delete *f_delete;
  container_f_hash   *f_hash;
};

struct hashset_iterator {
  struct hashset           *hashset;
  size_t                    bucket_i;
  struct hashset_list_node *node;

  void *(*get)(struct hashset_iterator *self);
  int (*next)(struct hashset_iterator *self);
  int (*end)(struct hashset_iterator *self);
};

typedef struct hashset_iterator   hashset_it;
typedef struct hashset_list_node *hashset_bit;
typedef struct hashset_bucket     hashset_bucket;
typedef struct hashset_list_node  hashset_list_node;
typedef struct hashset            hashset, *p_hashset;

p_hashset  hashset_new(container_f_cmp, container_f_new, container_f_delete,
                       container_f_hash);
void       hashset_free(p_hashset self);
hashset_it hashset_find(p_hashset self, void *data);
void       hashset_insert(p_hashset self, void *data);
hashset_it hashset_begin(p_hashset self);
void       hashset_replace(p_hashset self, hashset_it it, void *data);
