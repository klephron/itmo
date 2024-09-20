#include <criterion/criterion.h>

Test(simple, test) {
  cr_expect(1, "Hello world");
}
