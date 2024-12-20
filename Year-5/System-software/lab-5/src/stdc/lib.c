#include "lib.h"

#include <ctype.h>
#include <stdio.h>
#include <string.h>

static char *trim_end(char *str) {
  if (*str) {
    char *end = str + strlen(str) - 1;

    while (*end && isspace((unsigned char)*end)) {
      end--;
    }
    *(end + 1) = '\0';
  }
  return str;
}

uint8_t read_bool() {
  uint8_t v;
  scanf("%s", &v);
  return v;
}

uint8_t read_byte() {
  uint8_t v;
  scanf("%s", &v);
  return v;
}

uint8_t read_char() {
  uint8_t v;
  scanf(" %c", &v);
  return v;
}

int32_t read_int() {
  int32_t v;
  scanf("%d", &v);
  return v;
}

uint32_t read_uint() {
  uint32_t v;
  scanf("%u", &v);
  return v;
}

int64_t read_long() {
  int64_t v;
  scanf("%ld", &v);
  return v;
}

uint64_t read_ulong() {
  uint64_t v;
  scanf("%ld", &v);
  return v;
}

uint8_t *read_string() {
  char  *line = NULL;
  size_t len = 0;
  getline(&line, &len, stdin);
  if (line == NULL) {
    return NULL;
  }
  line = trim_end(line);
  return (uint8_t *)line;
}
