#include <criterion/criterion.h>

#include "util/util.h"

Test(simple, test) {
  cr_assert(0, "Hello, world");
}
