#pragma once

#include "util/container.h"

struct string_string {
  char *key;
  char *value;
};

typedef struct string_string string_string;

container_f_cmp container_cmp_string_string;
container_f_cmp container_cmp_chars;
container_f_cmp container_cmp_false;

container_f_new container_new_string_string;
container_f_new container_new_chars;
container_f_new container_new_move;

container_f_delete container_delete_string_string;
container_f_delete container_delete_chars;
