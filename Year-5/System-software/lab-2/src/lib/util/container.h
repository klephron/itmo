#pragma once

#include <stddef.h>

typedef int   container_f_cmp(const void *lsv, const void *rsv);
typedef void *container_f_new(void *data);
typedef void  container_f_delete(void *data);

typedef size_t container_f_hash(const void *data);
