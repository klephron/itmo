#include <cstdio>
#include <cstdlib>

#define input_open() (fopen("input.txt", "r"))
#define output_open() (fopen("output.txt", "w"))

// #include "helpers.hpp"

static inline void rebuild_tree(int* tree, const int tree_s) {
  for (int i = (tree_s / 2) - 2; i >= 0; --i) {
    tree[i] = tree[2 * i + 1] + tree[2 * i + 2];
  }
}

static inline int mid_tree(int tl, int tr) {
  return tl + (tr - tl) / 2;
}

static int query_tree_util(int* tree, int i, int tl, int tr, int l, int r) {
  if (l <= tl && tr <= r) {
    return tree[i];
  } else if (tr < l || tl > r) {
    return 0;
  } else {
    int mid = mid_tree(tl, tr);
    return query_tree_util(tree, 2 * i + 1, tl, mid, l, r) +
           query_tree_util(tree, 2 * i + 2, mid + 1, tr, l, r);
  }
}

// n is 2^k
static inline int query_tree(int* tree, int n, int a, int b) {
  return query_tree_util(tree, 0, 0, n - 1, a, b);
}

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

    // segment tree
    int tree_s = 1;
    while (tree_s < n) {
      tree_s *= 2;
    }

    tree_s *= 2;

    int* tree = (int*)malloc(sizeof(int) * tree_s);
    int* arr = tree + (tree_s / 2 - 1);

    {
      // build tree
      int i;
      for (i = 0; i < n; ++i) {
        fscanf(input, "%d", arr + i);
      }
      for (; i <= tree_s / 2; ++i) {
        arr[i] = 0;
      }
      rebuild_tree(tree, tree_s);
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
        if (a % 2 == 1) {
          rebuild_tree(tree, tree_s);
        }
      } else  // sum [a;b]
      {
        int s = query_tree(tree, tree_s / 2, a, b);
        fprintf(output, "%d\n", s);
      }
    }

    fprintf(output, "\n");

    free(tree);
  }

  fclose(output);
  fclose(input);

  return 0;
}
