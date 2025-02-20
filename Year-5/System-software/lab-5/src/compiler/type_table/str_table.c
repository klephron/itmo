#include "str.h"

#include "compiler/span/str.h"
#include "compiler/type_table/type_table.h"
#include "util/macro.h"
#include "util/math.h"
#include "util/strtable.h"
#include <stdio.h>
#include <string.h>

LIST_DECLARE_STATIC_INLINE(list_chars, char, container_cmp_false,
                           container_new_chars, container_delete_chars);

static inline void container_delete_list_chars(void *data) {
  list_chars_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_list_chars, list_chars, container_cmp_false,
                           container_new_move, container_delete_list_chars);

char *type_table_str(const type_table *table) {
  if (!table) {
    return NULL;
  }

  list_chars *header = list_chars_new();
  list_chars_push_back(header, "ID(PTR)");
  list_chars_push_back(header, "NAME");
  list_chars_push_back(header, "TYPE");
  list_chars_push_back(header, "TYPE_MONO");
  list_chars_push_back(header, "SPAN");
  list_chars_push_back(header, "EXTRA");

  list_list_chars *body = list_list_chars_new();
  for (hashset_type_entry_it it = hashset_type_entry_begin(table->entries);
       !END(it); NEXT(it)) {
    list_chars       *row = list_chars_new();
    char              buf[64];
    const type_entry *entry = GET(it);

    // ID
    snprintf(buf, STRMAXLEN(buf), "%p", entry);
    list_chars_push_back(row, buf);
    // NAME
    list_chars_push_back(row, entry->name);
    // TYPE
    char *type_s = type_str(entry->type);
    if (type_s) {
      list_chars_push_back(row, type_s);
      free(type_s);
    } else {
      list_chars_push_back(row, "-");
    }
    // TYPE_SPEC
    type_mono_enum spec;
    if ((spec = type_get_mono(entry->type)) == TYPE_MONO_UNSET) {
      spec = type_calc_mono(entry->type);
    }
    list_chars_push_back(row, (char *)type_mono_enum_str(spec));
    // SPAN
    if (entry->span) {
      char *span_s = span_str(entry->span);
      list_chars_push_back(row, span_s);
      free(span_s);
    } else {
      list_chars_push_back(row, "built-in");
    }
    // EXTRA
    list_chars_push_back(row, "");

    list_list_chars_push_back(body, row);
  }

  char *result = strtable_build("TYPE TABLE", &header->list, &body->list);

  list_list_chars_free(body);
  list_chars_free(header);

  return result;
}
