#pragma once

#include "compiler/span/span.h"
#include "compiler/type_table/type.h"
#include "util/hashset.h"

typedef struct type_entry_struct {
  char      *name; // used inside scope resolution
  type_base *type;
  span      *span;
} type_entry;

type_entry *type_entry_new(char *id, type_base *type, span *span);
void        type_entry_free(type_entry *self);

static inline int container_cmp_type_entry(const void *lsv, const void *rsv) {
  return container_cmp_ptr(lsv, rsv);
}
static inline void container_delete_type_entry(void *lsv) {
  type_entry_free(lsv);
}
static inline uint64_t container_hash_type_entry(const void *lsv) {
  return container_hash_ptr(lsv);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_type_entry, type_entry,
                              container_cmp_type_entry, container_new_move,
                              container_delete_type_entry,
                              container_hash_type_entry);

typedef struct type_table_struct {
  hashset_type_entry *entries;
} type_table;

type_table *type_table_new();
void        type_table_free(type_table *self);

void type_table_insert(type_table *self, char *id, type_base *type, span *span);
