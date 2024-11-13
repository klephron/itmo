#include <stddef.h>

#define MATH(type)                                                             \
  static inline type type##_max(type a, type b) { return a > b ? a : b; }      \
  static inline type type##_min(type a, type b) { return a > b ? b : a; }

MATH(size_t)
