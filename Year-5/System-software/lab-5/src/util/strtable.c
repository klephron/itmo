#include "strtable.h"

#include "util/macro.h"
#include "util/math.h"
#include "util/strbuf.h"
#include <string.h>

static inline void strbuf_append_cell(strbuf *buffer, size_t max_len,
                                      const char *field, const char *space) {
  strbuf_append(buffer, field);
  for (size_t i = 0; i < max_len - strlen(field); ++i) {
    strbuf_append(buffer, space);
  }
}

char *strtable_build(const list *header, const list *rows) {
  strbuf *buffer = strbuf_new(0, 0);
  size_t  columns = 0;

  for (list_it it = list_begin((list *)header); !list_it_end(&it);
       list_it_next(&it)) {
    ++columns;
  }

  size_t *widths = MALLOCN(size_t, columns);
  for (size_t i = 0; i != columns; ++i) {
    widths[i] = 0;
  }

  {
    size_t i = 0;
    for (list_it it = list_begin((list *)header); !list_it_end(&it);
         list_it_next(&it), ++i) {
      widths[i] = strlen(list_it_get(&it));
    }
  }

  {
    for (list_it rows_it = list_begin((list *)rows); !list_it_end(&rows_it);
         list_it_next(&rows_it)) {
      size_t i = 0;
      for (list_it it = list_begin(list_it_get(&rows_it));
           !list_it_end(&it) && i != columns; list_it_next(&it), ++i) {
        widths[i] = max_size_t(widths[i], strlen(list_it_get(&it)));
      }
    }
  }

  // separator
  {
    strbuf_append(buffer, STRTABLE_TOPLEFT);
    if (columns) {
      strbuf_append_cell(buffer, widths[0], "", STRTABLE_HORIZ);

      for (size_t i = 1; i != columns; ++i) {
        strbuf_append(buffer, STRTABLE_CROSS_TOP);
        strbuf_append_cell(buffer, widths[i], "", STRTABLE_HORIZ);
      }
    }
    strbuf_append(buffer, STRTABLE_TOPRIGHT "\n");
  }

  // header
  {
    strbuf_append(buffer, STRTABLE_VERT);
    if (columns) {
      list_it it = list_begin((list *)header);

      strbuf_append_cell(buffer, widths[0], list_it_get(&it), STRTABLE_SPACE);
      list_it_next(&it);

      for (size_t i = 1; !list_it_end(&it); list_it_next(&it), ++i) {
        strbuf_append(buffer, STRTABLE_VERT);
        strbuf_append_cell(buffer, widths[i], list_it_get(&it), STRTABLE_SPACE);
      }
    }
    strbuf_append(buffer, STRTABLE_VERT "\n");
  }

  // rows
  for (list_it rows_it = list_begin((list *)rows); !list_it_end(&rows_it);
       list_it_next(&rows_it)) {

    // separator
    {
      strbuf_append(buffer, STRTABLE_CROSS_LEFT);
      if (columns) {
        strbuf_append_cell(buffer, widths[0], "", STRTABLE_HORIZ);

        for (size_t i = 1; i != columns; ++i) {
          strbuf_append(buffer, STRTABLE_CROSS_MID);
          strbuf_append_cell(buffer, widths[i], "", STRTABLE_HORIZ);
        }
      }
      strbuf_append(buffer, STRTABLE_CROSS_RIGHT "\n");
    }

    {
      strbuf_append(buffer, STRTABLE_VERT);

      list_it it = list_begin(list_it_get(&rows_it));
      size_t  i = 0;

      if (!list_it_end(&it)) {
        strbuf_append_cell(buffer, widths[0], list_it_get(&it), STRTABLE_SPACE);
        list_it_next(&it);
        ++i;
      }

      for (; !list_it_end(&it) && i != columns; list_it_next(&it), ++i) {
        strbuf_append(buffer, STRTABLE_VERT);
        strbuf_append_cell(buffer, widths[i], list_it_get(&it), STRTABLE_SPACE);
      }
      for (; i < columns; ++i) {
        strbuf_append(buffer, STRTABLE_VERT);
        strbuf_append_cell(buffer, widths[i], "", STRTABLE_SPACE);
      }

      strbuf_append(buffer, STRTABLE_VERT "\n");
    }
  }

  // separator
  {
    strbuf_append(buffer, STRTABLE_BOTLEFT);
    if (columns) {
      strbuf_append_cell(buffer, widths[0], "", STRTABLE_HORIZ);

      for (size_t i = 1; i != columns; ++i) {
        strbuf_append(buffer, STRTABLE_CROSS_BOT);
        strbuf_append_cell(buffer, widths[i], "", STRTABLE_HORIZ);
      }
    }
    strbuf_append(buffer, STRTABLE_BOTRIGHT "\n");
  }

  free(widths);
  return strbuf_detach(buffer);
}
