#include <cstdio>
#include <cstdlib>

// #include "helpers.hpp"

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

struct TreapNode {
  int priority;
  int size;
  long value;
  long sum;
  TreapNode *left, *right;
};

struct Treap {
  TreapNode* root;
};

struct Bounds {
  int even_l;
  int even_r;
  int odd_l;
  int odd_r;
};

typedef struct Bounds Bounds;
typedef struct TreapNode TreapNode;
typedef struct Treap Treap;

static Treap* create_treap() {
  Treap* self = (Treap*)malloc(sizeof(Treap));
  Treap treap = {
      .root = NULL,
  };
  *self = treap;
  return self;
}

static TreapNode* create_node(int value) {
  TreapNode* self = (TreapNode*)malloc(sizeof(TreapNode));
  TreapNode node = {
      .priority = rand(),
      .size = 1,
      .value = value,
      .sum = value,
      .left = NULL,
      .right = NULL,
  };
  *self = node;
  return self;
}

static void delete_node(TreapNode* p) {
  if (p != NULL) {
    free(p);
  }
}

static void delete_nodes(TreapNode* p) {
  if (p != NULL) {
    delete_nodes(p->left);
    delete_nodes(p->right);
    delete_node(p);
  }
}

static void delete_treap(Treap* self) {
  if (self != NULL) {
    delete_nodes(self->root);
    free(self);
  }
}

static long get_sum(TreapNode* p) {
  return p ? p->sum : 0;
}

static int get_size(TreapNode* p) {
  return p ? p->size : 0;
}

static void update(TreapNode* p) {
  if (p != NULL) {
    p->sum = get_sum(p->left) + p->value + get_sum(p->right);
    p->size = get_size(p->left) + 1 + get_size(p->right);
  }
}

// resulting in: [lsv(keys) .. rsv(keys)]
// lsv(keys) < rsv(keys)
static TreapNode* merge(TreapNode* l, TreapNode* r) {
  if (!l || !r) {
    return l ? l : r;
  }
  if (l->priority > r->priority) {
    l->right = merge(l->right, r);
    update(l);
    return l;
  } else {
    r->left = merge(l, r->left);
    update(r);
    return r;
  }
}

// lsv(keys) < rsv(keys)
static void split(TreapNode* p, int k, TreapNode** l, TreapNode** r) {
  if (!p) {
    *l = NULL;
    *r = NULL;
  } else {
    if (get_size(p->left) < k) {
      /*
       *     lsv=p
       *  p->l p->r=a'(result of recursive call, < k)   rsv=rsv'
       */
      *l = p;
      split(p->right, k - get_size(p->left) - 1, &p->right, r);
    } else {
      *r = p;
      split(p->left, k, l, &p->left);
    }
    update(*l);
    update(*r);
  }
}

__attribute__((unused)) static int size(Treap* self) {
  return get_size(self->root);
}

static void push_back(Treap* self, int value) {
  self->root = merge(self->root, create_node(value));
}

// static int get(Treap* self, int k) {
//   TreapNode *lt, *eq, *gt;
//   split(self->root, k, &lt, &gt);
//   split(gt, 1, &eq, &gt);
//   int r = eq->value;
//   self->root = merge(merge(lt, eq), gt);
//   return r;
// }

// static void erase(Treap* self, int k) {
//   TreapNode *lt, *eq, *gt;
//   split(self->root, k, &lt, &gt);
//   split(gt, 1, &eq, &gt);
//   self->root = merge(lt, gt);
// }

// static void erase_range(Treap* self, int l, int r) {
//   TreapNode *lt, *eq, *gt;
//   split(self->root, l, &lt, &gt);
//   split(gt, r - l + 1, &eq, &gt);
//   self->root = merge(lt, gt);
// }

static void print_nodes(TreapNode* p, int l) {
  if (p != NULL) {
    print_nodes(p->left, l + 1);
    for (int i = 0; i < l; ++i) {
      printf(" ");
    }
    printf("|priority=%d\tsize=%d\tvalue=%ld\tsum=%ld\t\n", p->priority, p->size, p->value, p->sum);
    print_nodes(p->right, l + 1);
  }
}

__attribute__((unused)) static void print(Treap* self) {
  print_nodes(self->root, 0);
}

//    A[a_lt .. a_eq .. a_gt] B[b_lt .. b_eq .. b_gt]
// -> A[a_lt .. b_eq .. a_gt] B[b_lt .. a_eq .. b_gt]
static void swap_ranges(Treap* a, Treap* b, int al, int ar, int bl, int br) {
  TreapNode *a_lt, *a_eq, *a_gt;
  TreapNode *b_lt, *b_eq, *b_gt;
  split(a->root, al, &a_lt, &a_gt);
  split(a_gt, ar - al + 1, &a_eq, &a_gt);
  split(b->root, bl, &b_lt, &b_gt);
  split(b_gt, br - bl + 1, &b_eq, &b_gt);

  a->root = merge(merge(a_lt, b_eq), a_gt);
  b->root = merge(merge(b_lt, a_eq), b_gt);
}

// if s > e -> don't include
Bounds get_bounds(int l, int r) {
  int even_l, even_r;
  int odd_l, odd_r;

  l += 2;  // fix near zero bounds, (so r may be -1)
  r += 2;

  even_l = (l + 1) / 2;
  even_r = r / 2;
  odd_l = l / 2;
  odd_r = (r - 1) / 2;

  return {
      .even_l = even_l - 1,
      .even_r = even_r - 1,
      .odd_l = odd_l - 1,
      .odd_r = odd_r - 1,
  };
}

static void swap_ranges(Treap* even, Treap* odd, int l, int r) {
  Bounds bounds = get_bounds(l, r);
  swap_ranges(even, odd, bounds.even_l, bounds.even_r, bounds.odd_l, bounds.odd_r);
}

static long sum(Treap* self, int l, int r) {
  TreapNode *lt, *eq, *gt;
  split(self->root, l, &lt, &gt);
  split(gt, r - l + 1, &eq, &gt);
  long result = get_sum(eq);
  self->root = merge(merge(lt, eq), gt);
  return result;
}

static long sum(Treap* even, Treap* odd, int l, int r) {
  long result = 0;

  auto bounds = get_bounds(l, r);
  if (bounds.even_l <= bounds.even_r) {
    result += sum(even, bounds.even_l, bounds.even_r);
  }
  if (bounds.odd_l <= bounds.odd_r) {
    result += sum(odd, bounds.odd_l, bounds.odd_r);
  }

  return result;
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

    Treap* treap_even = create_treap();
    Treap* treap_odd = create_treap();

    for (int i = 0; i < n; ++i) {
      int v;
      scanf("%d", &v);
      if (i % 2 == 0) {
        push_back(treap_even, v);
      } else {
        push_back(treap_odd, v);
      }
    }

    int op, a, b;
    for (int ri = 0; ri < r; ++ri) {
      scanf("%d %d %d", &op, &a, &b);
      a--, b--;

      if (op == 1)  // swap a and a+1, a+2 and a+3, ... (even length)
      {
        swap_ranges(treap_even, treap_odd, a, b);
      } else  // sum [a;b]
      {
        long result = sum(treap_even, treap_odd, a, b);
        printf("%ld\n", result);
      }
    }

    delete_treap(treap_odd);
    delete_treap(treap_even);

    printf("\n");
  }

  return 0;
}
