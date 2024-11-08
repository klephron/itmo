#include "container.h"

#include "exception.h"
#include "util/container_util.h"

list *list_exception_new() {
  return list_new(container_cmp_false, container_new_exception_move,
                  container_delete_exception);
}

void *container_new_exception_move(void *_data) {
  exception *exc = _data;
  return exc;
}

void container_delete_exception(void *_data) {
  if (_data) {
    exception *exc = _data;
    exception_free(exc);
  }
}
