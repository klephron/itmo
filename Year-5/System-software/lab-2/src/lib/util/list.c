#include <stdlib.h>
#include <string.h>

#include "util/list.h"
#include "util/macro.h"

static int list_has_next(list_iter this) {
  if (this->next != NULL) {
    return 1;
  }
  return 0;
}

static void *list_get(p_list this, const void *data) {
  for (list_iter it = this->head; it != NULL; it = it->next) {
    if (this->f_equals(it->data, data)) {
      return it->data;
    }
  }
  return NULL;
}

static void list_put(p_list this, void *data) {
  if (this->unique) {
    for (list_iter it = this->head; it != NULL; it = it->next) {
      if (!this->f_equals(it->data, data)) {
        this->f_delete(it->data);
        it->data = this->f_new(data);
        return;
      }
    }
  }

  list_iter it = this->head;

  if (it != NULL) {
    while (list_has_next(it)) {
      it = it->next;
    }
  }

  p_list_entry tail = MALLOC(list_entry);
  tail->data = this->f_new(data);
  tail->next = NULL;

  if (it == NULL) {
    this->head = tail;
  } else {
    it->next = tail;
  }
}

static size_t list_size(p_list this) {
  size_t size = 0;
  for (list_iter it = this->head; it != NULL; it = it->next) {
    ++size;
  }
  return size;
}

static void list_free(p_list this) {
  if (this != NULL) {
    list_iter next;
    for (list_iter it = this->head; it != NULL; it = next) {
      next = it->next;
      this->f_delete(it->data);
      free(it);
    }
    free(this);
  }
}

p_list list_new(list_func_equals equals, list_func_new new,
                list_func_delete delete, int unique) {
  p_list this = MALLOC(list);

  this->unique = unique;

  this->head = NULL;
  this->get = list_get;
  this->put = list_put;
  this->free = list_free;
  this->size = list_size;

  this->f_equals = equals;
  this->f_new = new;
  this->f_delete = delete;
  return this;
}
