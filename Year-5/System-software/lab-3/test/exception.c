#include <criterion/criterion.h>

#include "exception/exception.h"

Test(exception, basic) {
  exception *exc = exception_new(0, 0, "ordinary/file", 1000, 50, "exception",
                                 "ordinary error");
  char      *msg = exception_str(exc);
  free(msg);
  exception_free(exc);
}

Test(exception, basic2) {
  exception *exc = exception_new(0, 0, "ordinary/file", 1000, 50, "exception",
                                 "ordinary error");
  char      *msg = exception_str(exc);
  free(msg);
  exception_free(exc);
}
