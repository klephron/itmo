#pragma once

#include "compiler/type_table/type.h"
#include "compiler/type_table/type_table.h"

const char *type_enum_str(type_enum kind);

char *type_table_str(const type_table *table);
