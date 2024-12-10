#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
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

char *read_string() {
  char  *line = NULL;
  size_t len = 0;
  getline(&line, &len, stdin);
  if (line == NULL) {
    return NULL;
  }
  line = trim_end(line);
  return line;
}

int main() {
  char *cmd = NULL;
  int   exit = 0;

  while (!exit) {
    printf("(dbg) ");
    cmd = read_string();
    if (!strcmp(cmd, "exit")) {
      exit = 1;
    } else {
      printf("%s\n", cmd);
    }
    free(cmd);
  }
}
