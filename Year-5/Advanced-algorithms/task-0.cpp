#include <stdio.h>
#include <string.h>

int main() {
  int n;
  scanf("%d", &n);
  while (n--) {
    char str[256];
    scanf("%s", str);
    size_t size = strlen(str);

    int res = 1;

    if (size % 2 != 0) {
      res = 0;
    }

    for (size_t i = 0, j = size / 2; i < size / 2; ++i, ++j) {
      if (str[i] != str[j]) {
        res = 0;
      }
    }

    if (res) {
      printf("YES\n");
    } else {
      printf("NO\n");
    }
  }
}
