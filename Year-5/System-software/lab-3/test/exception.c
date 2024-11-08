#include "exception.h"
#include <criterion/criterion.h>

Test(exception, basic) {
  exception *exc = exception_new(0, 0, "ordinary/file", 1000, 50, "exception",
                                 "ordinary error");
  char      *msg = exception_to_chars(exc);
  free(msg);
  exception_free(exc);
}

Test(exception, basic2) {
  exception *exc = exception_new(0, 0, "ordinary/file", 1000, 50, "exception",
                                 "ordinary error");
  char      *msg = exception_to_chars(exc);
  free(msg);
  exception_free(exc);
}
