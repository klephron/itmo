// #pragma once

// #include <stddef.h>
// #include <stdint.h>

// typedef enum x86_64_type_enum {
//   X86_64_TYPE_VOID,
//   X86_64_TYPE_BOOL,
//   X86_64_TYPE_BYTE,
//   X86_64_TYPE_INT,
//   X86_64_TYPE_UINT,
//   X86_64_TYPE_LONG,
//   X86_64_TYPE_ULONG,
//   X86_64_TYPE_CHAR,
//   X86_64_TYPE_STRING,
//   X86_64_TYPE_ARRAY, // flat array, nested is 1-d flat with object inside
//   X86_64_TYPE_OBJECT,
// } x86_64_type_enum;

// typedef struct __attribute__((packed)) x86_64_array_meta_struct {
//   uint64_t size;
//   uint64_t count;
// } x86_64_array_meta;

// // meta       - metadata (like offsets inside raw data, array sizes, etc.)
// // prefix 'i' - index inside array
// typedef struct __attribute__((packed)) x86_64_type_entry_struct {
//   x86_64_type_enum kind;
//   union {
//     void              *meta;
//     x86_64_array_meta *meta_array;
//   };
//   uint64_t i_next;
//   uint64_t i_child;
//   uint8_t  pad[4];
// } x86_64_type_entry;

// // linearized tree structure
// typedef struct __attribute__((packed)) x86_64_type_struct {
//   uint64_t          count;
//   x86_64_type_entry entries[0];
// } x86_64_type;

// const char *x86_64_type_enum_str(x86_64_type_enum kind);

// x86_64_array_meta *x86_64_meta_array_new(uint64_t size, uint64_t count);
// void               x86_64_meta_array_free(x86_64_array_meta *self);

// void x86_64_type_entries_shift(uint64_t count, x86_64_type_entry *entries,
//                                int64_t offset);

// x86_64_type *x86_64_type_new(uint64_t count);
// x86_64_type *x86_64_type_new_simple(x86_64_type_enum kind);
// x86_64_type *x86_64_type_new_complex(x86_64_type_enum kind, x86_64_type *child,
//                                      void *meta);

// void         x86_64_type_free(x86_64_type *type);
// int          x86_64_type_equals(x86_64_type *lsv, x86_64_type *rsv);
// x86_64_type *x86_64_type_copy(const x86_64_type *type);
// // size - parent size, offset - of first entry
// x86_64_type *x86_64_type_copy_u_entry(uint64_t root_count, uint64_t offset,
//                                       const x86_64_type_entry *first);

// static inline x86_64_type_enum x86_64_type_kind(const x86_64_type *type) {
//   return type->entries[0].kind;
// }
