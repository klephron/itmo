#include <cmath>
#include <cstdio>
#include <cstdlib>

#define input_open() (fopen("input.txt", "r"))
#define output_open() (fopen("output.txt", "w"))

// index of block
static int block_i(const int i, const int bl_l) {
  return i / bl_l;
}

// offset inside block
static int block_o(const int i, const int bl_l) {
  return i % bl_l;
}

static int handle_inside(const int* arr, int* cur_k, int k, int il, int ir) {
  int res = -1;
  for (int i = il; i <= ir; ++i) {
    if (arr[i] == 0) {
      (*cur_k) += 1;
    }
    if ((*cur_k) == k) {
      res = i;
      break;
    }
  }
  return res;
}

// plain C for fun
int main() {
  FILE* input = input_open();
  FILE* output = output_open();

  int arr_l;
  fscanf(input, "%d", &arr_l);

  int* arr = (int*)malloc(sizeof(int) * arr_l);
  for (int i = 0; i < arr_l; ++i) {
    fscanf(input, "%d", &arr[i]);
  }

  // sqrt decomposition init
  const int bl_l = sqrt(arr_l + 0.0);
  const int bl_c = (arr_l + bl_l - 1) / bl_l;
  int* bl_arr = (int*)malloc(sizeof(int) * bl_c);

  for (int i = 0; i < arr_l; ++i) {
    int bl_i = block_i(i, bl_l);
    int bl_o = block_o(i, bl_l);

    if (bl_o == 0) {
      bl_arr[bl_i] = 0;
    }
    bl_arr[bl_i] += arr[i] == 0;
  }

  int m;
  fscanf(input, "%d", &m);
  for (int op_i = 0; op_i < m; ++op_i) {
    char op;
    fscanf(input, " %c", &op);  // skip optional white space
    if (op == 'u') {
      int i, v;
      fscanf(input, "%d%d", &i, &v);
      i -= 1;

      int bl_i = block_i(i, bl_l);
      int ch = 0;
      ch -= (arr[i] == 0);
      ch += (v == 0);
      arr[i] = v;
      bl_arr[bl_i] += ch;
    } else {
      int l, r, k;
      fscanf(input, "%d%d%d", &l, &r, &k);
      l -= 1;
      r -= 1;

      const int bl_il = block_i(l, bl_l);
      const int bl_ir = block_i(r, bl_l);

      int res = -1;
      int cur_k = 0;
      do {
        if (bl_il == bl_ir) {
          res = handle_inside(arr, &cur_k, k, l, r);
        } else {
          // handle l's block
          res = handle_inside(arr, &cur_k, k, l, bl_l * (bl_il + 1) - 1);
          if (res != -1) {
            break;
          }

          // handle between
          for (int i = bl_il + 1; i < bl_ir; ++i) {
            cur_k += bl_arr[i];
            if (cur_k >= k) {
              cur_k -= bl_arr[i];
              res = handle_inside(arr, &cur_k, k, bl_l * i, bl_l * (i + 1) - 1);
              break;
            }
          }
          if (res != -1) {
            break;
          }

          // handle r's block
          res = handle_inside(arr, &cur_k, k, bl_l * bl_ir, r);
        }
      } while (0);

      if (res != -1) {
        res += 1;
      }
      fprintf(output, "%d\n", res);
    }
  }

  free(bl_arr);
  free(arr);

  fclose(input);
  fclose(output);
}
