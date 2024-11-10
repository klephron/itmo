#pragma once

#include "exception.h"
#include "util/container_util.h"
#include "util/list.h"

static inline void *container_new_exception(void *_data) { return _data; }
static inline void  container_delete_exception(void *_data) {
   exception_free(_data);
}
LIST_DECLARE_STATIC_INLINE(list_exception, exception, container_cmp_false,
                           container_new_exception, container_delete_exception);
