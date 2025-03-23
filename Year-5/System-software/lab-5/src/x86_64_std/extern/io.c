#include "io.h"

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
  char buf[16];
  scanf("%15s", buf);

  if (strcmp(buf, "true")) {
    return 1;
  } else {
    return 0;
  }
}

uint8_t read_byte() {
  uint8_t v;
  scanf("%hhu", &v);
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
  scanf("%lu", &v);
  return v;
}

uint8_t *read_string() {
  char  *line = NULL;
  size_t len  = 0;
  getline(&line, &len, stdin);
  if (line == NULL) {
    return NULL;
  }
  line = trim_end(line);
  return (uint8_t *)line;
}

void write_bool(uint8_t v) {
  if (v) {
    printf("true");
  } else {
    printf("false");
  }
}
void write_byte(uint8_t v) { printf("%hhu", v); }
void write_char(uint8_t v) { printf("%c", v); }
void write_int(int32_t v) { printf("%d", v); }
void write_uint(uint32_t v) { printf("%u", v); }
void write_long(int64_t v) { printf("%ld", v); }
void write_ulong(uint64_t v) { printf("%lu", v); }
void write_string(const uint8_t *v) { printf("%s", v); }
