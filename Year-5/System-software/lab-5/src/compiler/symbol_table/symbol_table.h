#pragma once

#include "compiler/span/span.h"
#include "compiler/type_table/type_table.h"
#include "util/container_util.h"
#include "util/hashset.h"

typedef struct symbol_entry_struct {
  char       *name; // used inside scope resolution
  type_entry *type_ref;
  span       *span;
} symbol_entry;

symbol_entry *symbol_entry_new(char *id, type_entry *type_ref, span *span);
void          symbol_entry_free(symbol_entry *self);

static inline int container_cmp_symbol_entry(const void *lsv, const void *rsv) {
  return container_cmp_ptr(lsv, rsv);
}
static inline void container_delete_symbol_entry(void *lsv) {
  symbol_entry_free(lsv);
}
static inline uint64_t container_hash_symbol_entry(const void *lsv) {
  return container_hash_ptr(lsv);
}
HASHSET_DECLARE_STATIC_INLINE(hashset_symbol_entry, symbol_entry,
                              container_cmp_symbol_entry, container_new_move,
                              container_delete_symbol_entry,
                              container_hash_symbol_entry);

typedef struct symbol_table_struct {
  hashset_symbol_entry *entries;
} symbol_table;

symbol_table *symbol_table_new();
void          symbol_table_free(symbol_table *self);

void symbol_table_insert(symbol_table *self, char *name, type_entry *type_ref,
                         span *span);
