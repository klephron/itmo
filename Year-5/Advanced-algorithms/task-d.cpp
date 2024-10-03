#include <cstdio>
#include <cstdlib>

#define input_open() (fopen("input.txt", "r"))
#define output_open() (fopen("output.txt", "w"))

// #include "helpers.hpp"

int main() {
  FILE* input = input_open();
  FILE* output = output_open();

  int active = 1;
  int suite = 0;

  while (active) {
    int n, r;
    fscanf(input, "%d %d", &n, &r);

    if (n == 0 && r == 0) {
      active = 0;
      continue;
    }

    fprintf(output, "Suite %d:\n", ++suite);

    int* arr = (int*)malloc(sizeof(int) * n);
    for (int i = 0; i < n; ++i) {
      fscanf(input, "%d", arr + i);
    }

    int op, a, b;
    for (int ri = 0; ri < r; ++ri) {
      fscanf(input, "%d %d %d", &op, &a, &b);
      a--, b--;

      if (op == 1)  // swap a and a+1, a+2 and a+3, ... (even length)
      {
        int t;
        for (int i = a; i < b; i += 2) {
          t = arr[i];
          arr[i] = arr[i + 1];
          arr[i + 1] = t;
        }
      } else  // sum [a;b]
      {
        int s = 0;
        for (int i = a; i <= b; ++i) {
          s += arr[i];
        }
        fprintf(output, "%d\n", s);
      }
    }

    fprintf(output, "\n");

    free(arr);
  }

  fclose(output);
  fclose(input);

  return 0;
}
