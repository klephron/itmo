#pragma once

#include "x86_64/value.h"

typedef struct __attribute__((packed)) x86_64_data_ref_struct {
  x86_64_value *value;
} x86_64_data_ref;
