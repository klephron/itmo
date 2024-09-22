#pragma once

#include <stdint.h>

struct exception_struct {
  char *stream;
  uint32_t type;
  uint32_t line;
  uint32_t offset;
  char *name;
  char *message;

  void (*free)(struct exception_struct *);
};

typedef struct exception_struct exception;

struct exception_struct *exception_new(const char *stream, uint32_t type,
                                       uint32_t line, uint32_t offset,
                                       const char *name, const char *message);

char *exception_to_chars(struct exception_struct *exc);
