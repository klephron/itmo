#include <cstdio>
#include <cstdlib>
#include <cstring>

#define input_open() (fopen("input.txt", "r"))
#define output_open() (fopen("output.txt", "w"))

int main() {
  FILE* input = input_open();
  FILE* output = output_open();

  int n;
  fscanf(input, "%d", &n);

  const size_t digits_l = 2 * 10e5 + 2;
  short* digits = (short*)malloc(sizeof(short) * digits_l);
  memset(digits, 0, digits_l * sizeof(short));
  size_t ones_c = 0;

  while (n--) {
    char op;
    int s;
    fscanf(input, " %c %d", &op, &s);

    if (op == '+') {
      while (digits[s] == 1) {
        digits[s] = 0;
        ones_c -= 1;
        ++s;
      }
      digits[s] = 1;
      ones_c += 1;
    } else {
      while (digits[s] == 0) {
        digits[s] = 1;
        ones_c += 1;
        ++s;
      }
      digits[s] = 0;
      ones_c -= 1;
    }

    fprintf(output, "%lu\n", ones_c);
  }

  free(digits);

  fclose(output);
  fclose(input);
}
