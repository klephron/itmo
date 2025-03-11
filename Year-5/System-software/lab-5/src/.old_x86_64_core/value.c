// #include "value.h"

// #include "x86_64_core/number.h"

// #include <assert.h>
// #include <stdarg.h>
// #include <stddef.h>
// #include <stdio.h>
// #include <stdlib.h>
// #include <string.h>

// static size_t c_type_direct_size(x86_64_type_enum type);

// #define C_X86_64_VALUE_DIRECT(suffix, type_t, type_enum)                       \
//   x86_64_value __x86_64_value_##suffix(type_t value) {                         \
//     assert(sizeof(type_t) == c_type_direct_size(type_enum));                   \
//     x86_64_data *data =                                                        \
//         x86_64_data_new(sizeof(type_t), x86_64_type_new_simple(type_enum));    \
//     *(type_t *)data->raw = value;                                              \
//     return (x86_64_value){.data = data};                                       \
//   }

// static size_t c_type_direct_size(x86_64_type_enum type) {
//   switch (type) {
//     case X86_64_TYPE_BOOL:
//       return sizeof(uint8_t);
//     case X86_64_TYPE_BYTE:
//       return sizeof(uint8_t);
//     case X86_64_TYPE_INT:
//       return sizeof(int32_t);
//     case X86_64_TYPE_UINT:
//       return sizeof(uint32_t);
//     case X86_64_TYPE_LONG:
//       return sizeof(int64_t);
//     case X86_64_TYPE_ULONG:
//       return sizeof(uint64_t);
//     case X86_64_TYPE_CHAR:
//       return sizeof(char);
//     case X86_64_TYPE_VOID:
//       return 0;
//       break;
//     default:
//       fprintf(stderr, "error: type is only for direct types\n");
//       return 0;
//   }
// }

// x86_64_data *x86_64_data_new(size_t data_size, x86_64_type *type) {
//   x86_64_data *data = malloc(sizeof(x86_64_data) + data_size);
//   if (!data) {
//     fprintf(stderr, "error: malloc returned NULL\n");
//     return NULL;
//   }
//   data->type = type;
//   data->raw_size = data_size;
//   data->refs = 1;
//   return data;
// }

// void __x86_64_value_init(x86_64_value *ref) { ref->data = NULL; }

// void __x86_64_value_drop(x86_64_value *ref) {
//   x86_64_data *data = ref->data;
//   if (!data) {
//     return;
//   }

//   data->refs -= 1;
//   if (data->refs) {
//     return;
//   }

//   x86_64_type_free(data->type);

//   free(data);
//   ref->data = NULL;
// }

// // VALUE
// x86_64_value __x86_64_make_void() { return (x86_64_value){.data = NULL}; }

// C_X86_64_VALUE_DIRECT(bool, uint8_t, X86_64_TYPE_BOOL);
// C_X86_64_VALUE_DIRECT(byte, uint8_t, X86_64_TYPE_BYTE);
// C_X86_64_VALUE_DIRECT(int, int32_t, X86_64_TYPE_INT);
// C_X86_64_VALUE_DIRECT(uint, uint32_t, X86_64_TYPE_UINT);
// C_X86_64_VALUE_DIRECT(long, int64_t, X86_64_TYPE_LONG);
// C_X86_64_VALUE_DIRECT(ulong, uint64_t, X86_64_TYPE_ULONG);
// C_X86_64_VALUE_DIRECT(char, uint8_t, X86_64_TYPE_CHAR);

// x86_64_value __x86_64_value_string(const uint8_t *value) {
//   size_t sz;
//   if (value == NULL) {
//     sz = 0;
//     value = (const uint8_t *)"";
//   } else {
//     sz = strlen((char *)value);
//   }

//   x86_64_data *data =
//       x86_64_data_new(sz + 1, x86_64_type_new_simple(X86_64_TYPE_STRING));
//   memcpy(data->raw, value, sz + 1);

//   return (x86_64_value){.data = data};
// }

// x86_64_value __x86_64_value_string_move(uint8_t *value) {
//   x86_64_value r = __x86_64_value_string(value);
//   free(value);
//   return r;
// }

// x86_64_value __x86_64_value_array(const x86_64_type *elem_type, ...) {
//   x86_64_type_enum elem_kind = x86_64_type_kind(elem_type);

//   switch (elem_kind) {
//     case X86_64_TYPE_VOID:
//     case X86_64_TYPE_BOOL:
//     case X86_64_TYPE_BYTE:
//     case X86_64_TYPE_INT:
//     case X86_64_TYPE_UINT:
//     case X86_64_TYPE_LONG:
//     case X86_64_TYPE_ULONG:
//     case X86_64_TYPE_CHAR:
//     case X86_64_TYPE_STRING:
//       break;
//     case X86_64_TYPE_ARRAY:
//     case X86_64_TYPE_OBJECT:
//       fprintf(stderr, "error: element type should be primitive, got %s\n",
//               x86_64_type_enum_str(elem_kind));
//       return __x86_64_make_void();
//     default:
//       fprintf(stderr, "error: unknown type, got %s\n",
//               x86_64_type_enum_str(elem_kind));
//       return __x86_64_make_void();
//       break;
//   }

//   va_list args;
//   va_start(args, elem_type);

//   uint64_t data_size = c_type_direct_size(elem_kind);
//   size_t   rank = 0;
//   while (1) {
//     x86_64_value arg = va_arg(args, x86_64_value);
//     if (!arg.data) {
//       break;
//     }
//     uint64_t count = x86_64_number_unsigned(x86_64_number_get(arg.data));
//     if (count == UINT64_MAX) {
//       fprintf(stderr, "error: with arg data %p while creating array\n",
//               arg.data);
//       goto error;
//     }
//     data_size *= count;
//     ++rank;
//   }

//   x86_64_type *type = x86_64_type_new(elem_type->count + rank);
//   memcpy(type->entries + rank, elem_type->entries,
//          sizeof(x86_64_type_entry) * elem_type->count);
//   x86_64_type_entries_shift(elem_type->count, type->entries + rank, rank);

//   va_start(args, elem_type);

//   uint64_t entry_size = data_size;

//   for (uint64_t i = 0; i < rank; ++i) {
//     x86_64_value arg = va_arg(args, x86_64_value);
//     uint64_t     count = x86_64_number_unsigned(x86_64_number_get(arg.data));
//     entry_size = entry_size / count;

//     type->entries[i] = (x86_64_type_entry){
//         .kind = X86_64_TYPE_ARRAY,
//         .i_child = i + 1,
//         .i_next = 0,
//         .meta = x86_64_meta_array_new(entry_size, count),
//     };
//   }

//   va_end(args);

//   x86_64_data *data = x86_64_data_new(data_size, type);
//   return (x86_64_value){.data = data};

// error:
//   va_end(args);
//   return __x86_64_make_void();
// }

// uint8_t __x86_64_unwrap_bool(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_BOOL) {
//     uint8_t v = *(uint8_t *)data->raw;
//     return v;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected bool(type %d)\n",
//           kind, X86_64_TYPE_BOOL);

// error:
//   return 0;
// }

// uint8_t __x86_64_unwrap_byte(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_BYTE) {
//     uint8_t v = *(uint8_t *)data->raw;
//     return v;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected byte(type %d)\n",
//           kind, X86_64_TYPE_BYTE);

// error:
//   return 0;
// }

// int32_t __x86_64_unwrap_int(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_INT) {
//     int32_t v = *(int32_t *)data->raw;
//     return v;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected int(type %d)\n",
//           kind, X86_64_TYPE_INT);

// error:
//   return 0;
// }

// uint32_t __x86_64_unwrap_uint(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_UINT) {
//     uint32_t v = *(uint32_t *)data->raw;
//     return v;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected uint(type %d)\n",
//           kind, X86_64_TYPE_UINT);

// error:
//   return 0;
// }

// int64_t __x86_64_unwrap_long(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_LONG) {
//     int64_t v = *(int64_t *)data->raw;
//     return v;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected long(type %d)\n",
//           kind, X86_64_TYPE_LONG);

// error:
//   return 0;
// }

// uint64_t __x86_64_unwrap_ulong(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_ULONG) {
//     uint64_t v = *(uint64_t *)data->raw;
//     return v;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected ulong(type %d)\n",
//           kind, X86_64_TYPE_ULONG);

// error:
//   return 0;
// }

// uint8_t __x86_64_unwrap_char(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_CHAR) {
//     uint8_t v = *(uint8_t *)data->raw;
//     return v;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected char(type %d)\n",
//           kind, X86_64_TYPE_CHAR);

// error:
//   return 0;
// }

// uint8_t *__x86_64_unwrap_string(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_STRING) {
//     return data->raw;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected string(type %d)\n",
//           kind, X86_64_TYPE_STRING);

// error:
//   return NULL;
// }

// void *__x86_64_unwrap_array(x86_64_value value) {
//   if (!value.data) {
//     fprintf(stderr, "error: cond from null value\n");
//     goto error;
//   }

//   x86_64_data     *data = value.data;
//   x86_64_type_enum kind = x86_64_type_kind(data->type);

//   if (kind == X86_64_TYPE_ARRAY) {
//     return data->raw;
//   }

//   fprintf(stderr, "error: unsupported cond type %d, expected array(type %d)\n",
//           kind, X86_64_TYPE_ARRAY);

// error:
//   return NULL;
// }
