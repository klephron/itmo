#pragma once

#include "util/list.h"

list *list_exception_new();

void *container_new_exception_move(void *_data);
void  container_delete_exception(void *_data);
