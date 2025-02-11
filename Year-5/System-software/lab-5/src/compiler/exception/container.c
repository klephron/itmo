#include "container.h"

size_t list_exception_count_by_type(const list_exception *list,
                                    exception_type        type) {
  size_t cnt = 0;
  for (list_exception_it it = list_exception_begin((list_exception *)list);
       !it.end(&it); it.next(&it)) {
    exception *exc = it.get(&it);
    if (exc->type == type) {
      ++cnt;
    }
  }
  return cnt;
}

size_t list_exception_count_by_level(const list_exception *list,
                                     exception_level       level) {
  if (!list) {
    return 0;
  }

  size_t cnt = 0;
  for (list_exception_it it = list_exception_begin((list_exception *)list);
       !it.end(&it); it.next(&it)) {
    exception *exc = it.get(&it);
    if (exc->level == level) {
      ++cnt;
    }
  }
  return cnt;
}
