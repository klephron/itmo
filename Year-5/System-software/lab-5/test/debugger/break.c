#include <criterion/criterion.h>

#include "debugger/context/context.h"

#include <stdint.h>
#include <stdio.h>

Test(debugger, break_1) {
  hashset_breakpoint *breaks = hashset_breakpoint_new();

  breakpoint *b1 = breakpoint_new(1, 0, 1, NULL, UINT64_MAX);
  breakpoint *b2 = breakpoint_new(2, 12, 1, NULL, UINT64_MAX);

  hashset_breakpoint_insert(breaks, b1);

  hashset_breakpoint_it it = hashset_breakpoint_find(breaks, b2);
  cr_expect_eq(it.end(&it), false);

  breakpoint_free(b2);
  hashset_breakpoint_free(breaks);
}
