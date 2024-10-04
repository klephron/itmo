#include <cstdio>
#include <cstdlib>

// #include "helpers.hpp"

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

int main() {
  stdin_input();
  stdout_output();

  int active = 1;
  int suite = 0;

  while (active) {
    int n, r;
    scanf("%d %d", &n, &r);

    if (n == 0 && r == 0) {
      active = 0;
      continue;
    }

    printf("Suite %d:\n", ++suite);

    for (int i = 0; i < n; ++i) {
      int k;
      scanf("%d", &k);
    }

    int op, a, b;
    for (int ri = 0; ri < r; ++ri) {
      scanf("%d %d %d", &op, &a, &b);
      a--, b--;

      if (op == 1)  // swap a and a+1, a+2 and a+3, ... (even length)
      {
      } else  // sum [a;b]
      {
      }
    }

    printf("\n");
  }

  return 0;
}
