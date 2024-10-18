#include "util/hashset.h"

#include "util/macro.h"
#include <stdlib.h>

static void hashset_bucket_free_in(p_hashset              self,
                                   struct hashset_bucket *bucket) {
  hashset_list_node *next;
  for (hashset_list_node *it = bucket->head; it; it = next) {
    next = it->next;
    self->f_delete(it->data);
    free(it);
  }
}

p_hashset hashset_new(container_f_cmp f_cmp, container_f_new f_new,
                      container_f_delete f_delete, container_f_hash f_hash) {
  p_hashset self = MALLOC(hashset);

  self->load_factor = HASHTABLE_LOAD_FACTOR;
  self->growth_factor = HASHTABLE_GROWTH_FACTOR;
  self->capacity = HASHTABLE_INITIAL_CAPACITY;
  self->size = 0;

  self->f_cmp = f_cmp;
  self->f_new = f_new;
  self->f_delete = f_delete;
  self->f_hash = f_hash;

  self->buckets = MALLOCN(hashset_bucket, self->capacity);
  for (size_t i = 0; i < self->capacity; ++i) {
    self->buckets[i].head = NULL;
  }

  return self;
}

void hashset_free(p_hashset self) {
  if (self) {
    for (size_t i = 0; i < self->capacity; ++i) {
      hashset_bucket_free_in(self, self->buckets + i);
    }
    free(self->buckets);
    free(self);
  }
}

static int hashset_iterator_end(hashset_it *it) { return !it->node; }

static void *hashset_iterator_get(hashset_it *it) { return it->node->data; }

static int hashset_iterator_next(hashset_it *it) {
  if (it->node) {
    it->node = it->node->next;
    if (!it->node) {
      while (++it->bucket_i < it->hashset->capacity &&
             !it->hashset->buckets[it->bucket_i].head)
        ;
      if (it->bucket_i == it->hashset->capacity) {
        return 0;
      }
      it->node = it->hashset->buckets[it->bucket_i].head;
    }
    return 1;
  }
  return 0;
}

static hashset_it hashset_iterator_ctr(p_hashset hashset, size_t bucket_i,
                                       hashset_bit node) {
  hashset_it it = {.hashset = hashset,
                   .bucket_i = bucket_i,
                   .node = node,
                   .end = hashset_iterator_end,
                   .get = hashset_iterator_get,
                   .next = hashset_iterator_next};
  return it;
}

hashset_it hashset_find(p_hashset self, void *data) {
  size_t                 index = self->f_hash(data) % self->capacity;
  struct hashset_bucket *bucket = self->buckets + index;

  for (hashset_bit it = bucket->head; it; it = it->next) {
    if (!self->f_cmp(it->data, data)) {
      return hashset_iterator_ctr(self, index, it);
    }
  }
  return hashset_iterator_ctr(self, 0, NULL);
}

static void hashset_grow(p_hashset self) {
  size_t new_capacity = self->capacity * self->growth_factor;

  hashset_bucket *new_buckets = MALLOCN(hashset_bucket, new_capacity);
  for (size_t i = 0; i < new_capacity; ++i) {
    new_buckets[i].head = NULL;
  }

  for (size_t i = 0; i < self->capacity; ++i) {
    hashset_bucket *bucket = self->buckets + i;
    hashset_bit     next;
    for (hashset_bit it = bucket->head; it; it = next) {
      next = it->next;
      size_t          new_i = self->f_hash(it->data) % new_capacity;
      hashset_bucket *new_bucket = new_buckets + new_i;
      it->next = new_bucket->head;
      new_bucket->head = it;
    }
  }

  free(self->buckets);
  self->buckets = new_buckets;
  self->capacity = new_capacity;
}

void hashset_insert(p_hashset self, void *data) {
  if ((float)self->size / self->capacity > self->load_factor) {
    hashset_grow(self);
  }

  size_t          index = self->f_hash(data) % self->capacity;
  hashset_bucket *bucket = self->buckets + index;

  for (hashset_bit it = bucket->head; it; it = it->next) {
    if (!self->f_cmp(it->data, data)) {
      self->f_delete(it->data);
      it->data = self->f_new(data);
      return;
    }
  }

  hashset_list_node *node = MALLOC(hashset_list_node);
  node->data = self->f_new(data);
  node->next = bucket->head;
  bucket->head = node;
  self->size += 1;
}

hashset_it hashset_begin(p_hashset self) {
  for (size_t i = 0; i < self->capacity; ++i) {
    hashset_bucket *bucket = self->buckets + i;
    if (bucket->head) {
      return hashset_iterator_ctr(self, i, bucket->head);
    }
  }
  return hashset_iterator_ctr(self, 0, NULL);
}
