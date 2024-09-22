#pragma once

#include "util/list.h"

int list_string_equals(const void *lsv, const void *rsv);

int list_equals_false(const void *lsv, const void *rsv);

list_data *list_string_string_new(const void *_lsv, const void *_rsv);

void list_string_string_delete(list_data *data);
