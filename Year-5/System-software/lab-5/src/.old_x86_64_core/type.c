// #include "type.h"

// #include <stdio.h>
// #include <stdlib.h>
// #include <string.h>

// const char *x86_64_type_enum_str(x86_64_type_enum kind) {
//   switch (kind) {
//     case X86_64_TYPE_VOID:
//       return "void";
//     case X86_64_TYPE_BOOL:
//       return "bool";
//     case X86_64_TYPE_BYTE:
//       return "byte";
//     case X86_64_TYPE_INT:
//       return "int";
//     case X86_64_TYPE_UINT:
//       return "uint";
//     case X86_64_TYPE_LONG:
//       return "long";
//     case X86_64_TYPE_ULONG:
//       return "ulong";
//     case X86_64_TYPE_CHAR:
//       return "char";
//     case X86_64_TYPE_STRING:
//       return "string";
//     case X86_64_TYPE_ARRAY:
//       return "array";
//     case X86_64_TYPE_OBJECT:
//       return "object";
//     default:
//       return "<unknown>";
//   }
// }

// x86_64_array_meta *x86_64_meta_array_new(uint64_t size, uint64_t count) {
//   x86_64_array_meta *self = malloc(sizeof(x86_64_array_meta));
//   if (!self) {
//     fprintf(stderr, "error: malloc returned NULL\n");
//     return NULL;
//   }
//   self->size = size;
//   self->count = count;
//   return self;
// }

// void x86_64_meta_array_free(x86_64_array_meta *self) {
//   if (self) {
//     free(self);
//   }
// }

// void x86_64_type_entries_shift(size_t count, x86_64_type_entry *entries,
//                                int64_t offset) {
//   for (size_t i = 0; i < count; ++i) {
//     if (entries[i].i_child) {
//       entries[i].i_child += offset;
//     }
//     if (entries[i].i_next) {
//       entries[i].i_next += offset;
//     }
//   }
// }

// void x86_64_type_free(x86_64_type *type) {
//   if (type) {
//     for (size_t i = 0; i < type->count; ++i) {
//       switch (type->entries[i].kind) {
//         case X86_64_TYPE_ARRAY:
//           x86_64_meta_array_free(type->entries[i].meta_array);
//           break;
//         default:
//           break;
//       }
//     }
//     free(type);
//   }
// }

// x86_64_type *x86_64_type_new(uint64_t count) {
//   x86_64_type *self =
//       malloc(sizeof(x86_64_type) + sizeof(x86_64_type_entry) * count);
//   self->count = count;
//   return self;
// }

// x86_64_type *x86_64_type_new_simple(x86_64_type_enum kind) {
//   switch (kind) {
//     case X86_64_TYPE_ARRAY:
//       fprintf(stderr, "error: array is not simple type");
//       return NULL;
//     case X86_64_TYPE_OBJECT:
//       fprintf(stderr, "error: object is not simple type");
//       return NULL;
//     default: {
//       x86_64_type *self = x86_64_type_new(1);
//       self->count = 1;
//       self->entries[0] = (x86_64_type_entry){
//           .kind = kind,
//           .meta = NULL,
//           .i_next = 0,
//           .i_child = 0,
//       };
//       return self;
//     }
//   }
// }

// x86_64_type *x86_64_type_new_complex(x86_64_type_enum kind, x86_64_type *child,
//                                      void *meta) {
//   switch (kind) {
//     case X86_64_TYPE_ARRAY: {
//       x86_64_type *self = x86_64_type_new(child->count + 1);
//       self->entries[0] = (x86_64_type_entry){
//           .kind = kind,
//           .meta_array = meta,
//           .i_child = 1,
//           .i_next = 0,
//       };
//       memcpy(self->entries + 1, child->entries,
//              child->count * sizeof(x86_64_type_entry));
//       x86_64_type_entries_shift(child->count, self->entries + 1, 1);
//       return self;
//     } break;
//     case X86_64_TYPE_VOID:
//     case X86_64_TYPE_BOOL:
//     case X86_64_TYPE_BYTE:
//     case X86_64_TYPE_INT:
//     case X86_64_TYPE_UINT:
//     case X86_64_TYPE_LONG:
//     case X86_64_TYPE_ULONG:
//     case X86_64_TYPE_CHAR:
//     case X86_64_TYPE_STRING: {
//       fprintf(stderr, "error: %s(%d) is a simple type",
//               x86_64_type_enum_str(kind), kind);
//       return NULL;
//     }
//     case X86_64_TYPE_OBJECT: {
//       fprintf(stderr, "TODO: object");
//       return NULL;
//     }
//     default:
//       fprintf(stderr, "error: unsupported type %s(%d)",
//               x86_64_type_enum_str(kind), kind);
//       return NULL;
//   }
// }

// int x86_64_type_equals(x86_64_type *lsv, x86_64_type *rsv) {
//   if (lsv->count != rsv->count) {
//     return 0;
//   }
//   return !memcmp(lsv->entries, rsv->entries,
//                  lsv->count * sizeof(x86_64_type_entry));
// }

// x86_64_type *x86_64_type_copy(const x86_64_type *type) {
//   if (!type) {
//     return NULL;
//   }

//   size_t e_sz = type->count * sizeof(x86_64_type_entry);

//   x86_64_type *self = x86_64_type_new(type->count);
//   memcpy(self->entries, type->entries, e_sz);
//   return self;
// }

// x86_64_type *x86_64_type_copy_u_entry(uint64_t root_count, uint64_t offset,
//                                       const x86_64_type_entry *first) {

//   size_t count = root_count - offset;
//   size_t e_sz = count * sizeof(x86_64_type_entry);

//   x86_64_type *self = x86_64_type_new(count);
//   memcpy(self->entries, first, e_sz);
//   x86_64_type_entries_shift(count, self->entries, -offset);

//   return self;
// }
