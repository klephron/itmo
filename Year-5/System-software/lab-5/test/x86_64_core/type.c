#include <criterion/criterion.h>

#include "x86_64_core/type.h"

Test(x86_64_core_type, test_1) {
  x86_64_type *simple = x86_64_type_new_simple(X86_64_TYPE_INT);
  cr_expect_eq(simple->count, 1);
  cr_expect_eq(simple->entries[0].i_child, 0);
  cr_expect_eq(simple->entries[0].i_next, 0);
  cr_expect_eq(simple->entries[0].meta, NULL);
  cr_expect_eq(simple->entries[0].kind, X86_64_TYPE_INT);
  x86_64_type_free(simple);
}

Test(x86_64_core_type, test_2) {
  x86_64_type *simple = x86_64_type_new_simple(X86_64_TYPE_INT);

  x86_64_array_meta *meta = x86_64_meta_array_new(8, 16);
  cr_expect_eq(meta->count, 16);
  cr_expect_eq(meta->size, 8);

  x86_64_type *complex =
      x86_64_type_new_complex(X86_64_TYPE_ARRAY, simple, meta);
  cr_expect_eq(complex->count, 2);
  cr_expect_eq(complex->entries[0].i_child, 1);
  cr_expect_eq(complex->entries[0].i_next, 0);
  cr_expect_eq(complex->entries[0].meta, meta);
  cr_expect_eq(complex->entries[0].kind, X86_64_TYPE_ARRAY);
  cr_expect_eq(complex->entries[1].i_child, 0);
  cr_expect_eq(complex->entries[1].i_next, 0);
  cr_expect_eq(complex->entries[1].meta, NULL);
  cr_expect_eq(complex->entries[1].kind, X86_64_TYPE_INT);

  x86_64_type_free(complex);
  x86_64_type_free(simple);
}
