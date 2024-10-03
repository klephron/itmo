#include <cstdio>
#include <cstdlib>

#define input_open() (fopen("input.txt", "r"))
#define output_open() (fopen("output.txt", "w"))

#include "helpers.hpp"

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
      if (op == 1) {
        // handle op 1
      } else {
        // handle op 2
      }
    }

    free(arr);
  }

  fclose(output);
  fclose(input);

  return 0;
}
