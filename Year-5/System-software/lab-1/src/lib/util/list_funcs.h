#pragma once

#include "util/list.h"

int string_equals(const void *lsv, const void *rsv);

list_data *string_string_new(const void *_lsv, const void *_rsv);

void string_string_delete(list_data *data);
