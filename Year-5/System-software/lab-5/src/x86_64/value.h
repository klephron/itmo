#pragma once

#include "x86_64/data.h"

typedef struct __attribute__((packed)) x86_64_value_struct x86_64_value;

struct __attribute__((packed)) x86_64_value_struct {
  x86_64_data *data;
};
