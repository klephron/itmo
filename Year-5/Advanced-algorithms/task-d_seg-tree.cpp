#include <cstdio>
#include <cstdlib>

// #include "helpers.hpp"

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

static inline void build_tree(int* tree, const int tree_s) {
  for (int i = (tree_s / 2) - 2; i >= 0; --i) {
    tree[i] = tree[2 * i + 1] + tree[2 * i + 2];
  }
}

static inline int mid_tree(const int tl, const int tr) {
  return tl + (tr - tl) / 2;
}

static inline int rebuild_tree_util(int* tree, int i, int tl, int tr, const int l, const int r) {
  if (tr < l || tl > r) {
    return tree[i];
  } else if (tr - tl == 1) {
    return tree[i] = tree[2 * i + 1] + tree[2 * i + 2];
  }
  int mid = mid_tree(tl, tr);
  int sl = rebuild_tree_util(tree, 2 * i + 1, tl, mid, l, r);
  int sr = rebuild_tree_util(tree, 2 * i + 2, mid + 1, tr, l, r);
  return tree[i] = sl + sr;
}

static int query_tree_util(const int* tree, int i, int tl, int tr, const int l, const int r) {
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

static inline void rebuild_tree(int* tree, int n, int a, int b) {
  rebuild_tree_util(tree, 0, 0, n - 1, a, b);
}

// n is 2^k
static inline int query_tree(int* tree, int n, int a, int b) {
  return query_tree_util(tree, 0, 0, n - 1, a, b);
}

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
        scanf("%d", arr + i);
      }
      for (; i <= tree_s / 2; ++i) {
        arr[i] = 0;
      }
      build_tree(tree, tree_s);
    }

    int op, a, b;
    for (int ri = 0; ri < r; ++ri) {
      scanf("%d %d %d", &op, &a, &b);
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
          rebuild_tree(tree, tree_s / 2, a, b);
        }
      } else  // sum [a;b]
      {
        int s = query_tree(tree, tree_s / 2, a, b);
        printf("%d\n", s);
      }
    }

    printf("\n");

    free(tree);
  }

  return 0;
}
