#include "util/string.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

char *escape_html(const char *str) {
  size_t len = strlen(str);
  size_t new_len = len * 6; // worst case

  char *out = (char *)malloc(new_len + 1);
  if (!out) {
    return NULL;
  }

  char *end = out;
  for (size_t i = 0; i < len; ++i) {
    const char *after = NULL;

    switch (str[i]) {
    case '&':
      after = "&amp;";
      break;
    case '<':
      after = "&lt;";
      break;
    case '>':
      after = "&gt;";
      break;
    case '"':
      after = "&quot;";
      break;
    case '\'':
      after = "&#39;";
      break;
    case '[':
      after = "&#91;";
      break;
    case ']':
      after = "&#93;";
      break;
    }

    if (after == NULL) {
      end[0] = str[i];
      end[1] = '\0';
      end += 1;
    } else {
      end = stpcpy(end, after);
    }
  }

  return out;
}
