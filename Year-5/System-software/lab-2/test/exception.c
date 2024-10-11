#include "exception.h"
#include <criterion/criterion.h>

Test(exception, basic) {
  exception *exc = exception_new("ordinary/file", 10, 1000, 50, "exception",
                                 "ordinary error");
  char *msg = exception_to_chars(exc);
  free(msg);
  exc->free(exc);
}

Test(exception, basic2) {
  exception *exc = exception_new("ordinary/file", 10012, 1000, 50, "exception",
                                 "ordinary error");
  char *msg = exception_to_chars(exc);
  free(msg);
  exc->free(exc);
}
