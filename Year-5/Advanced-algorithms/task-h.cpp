#include <climits>
#include <cstdio>
#include <iostream>
#include <vector>

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

template <typename T>
using V = typename std::vector<T>;

#define LCA_SIZE 200000
#define NODES_SIZE 100000

V<V<int32_t>> edges;
V<int32_t> visited;

V<V<int32_t>> tree_children;
V<int32_t> depths;
V<int32_t> lasts;

V<int32_t> depth_tree;

static int32_t seg_tree_query(int32_t i, int32_t tl, int32_t tr, const int32_t l, const int32_t r) {
  if (l <= tl && tr <= r) {
    return depth_tree[i];
  } else if (tr < l || tl > r) {
    return INT_MAX;
  } else {
    int32_t mid = tl + (tr - tl) / 2;
    int32_t left_i = seg_tree_query(2 * i + 1, tl, mid, l, r);
    int32_t right_i = seg_tree_query(2 * i + 2, mid + 1, tr, l, r);
    return left_i < right_i ? left_i : right_i;
  }
}

static inline void seg_tree_build() {
  size_t n = depths.size();

  size_t tr_sz = 1;
  while (tr_sz < n) {
    tr_sz *= 2;
  }
  tr_sz *= 2;

  depth_tree = V<int32_t>(tr_sz);

  size_t ti = (tr_sz / 2 - 1);
  for (size_t i = 0; i < n; ++i, ++ti) {
    depth_tree[ti] = depths[i];
  }
  for (; ti < tr_sz; ++ti) {
    depth_tree[ti] = INT_MAX;
  }
  for (ti = (tr_sz / 2) - 2;; --ti) {
    int32_t left_i = depth_tree[2 * ti + 1];
    int32_t right_i = depth_tree[2 * ti + 2];
    depth_tree[ti] = left_i < right_i ? left_i : right_i;
    if (ti == 0)
      break;
  }
}

// returns next i
static int32_t lca_setup_dfs(int32_t d, int32_t i, int32_t p) {
  depths[i] = d;
  for (auto ch : tree_children[p]) {
    i = lca_setup_dfs(d + 1, i + 1, ch);
    depths[i] = d;
  }
  lasts[p] = i;
  return i + 1;
}

static inline void lca_setup() {
  size_t n = tree_children.size();
  depths = V<int32_t>(n * 2 - 1);
  lasts = V<int32_t>(n);
  lca_setup_dfs(0, 0, 0);
}

static inline void tree_build_dfs(int32_t p) {
  visited[p] = 1;
  for (auto c : edges[p]) {
    if (!visited[c]) {
      tree_children[p].push_back(c);
      tree_build_dfs(c);
    }
  }
}

static inline void tree_build() {
  size_t n = edges.size();
  tree_children = std::vector(n, V<int32_t>());
  visited = std::vector<int32_t>(n, 0);
  tree_build_dfs(0);
}

static inline int32_t energy_required(int32_t l, int32_t r) {
  int32_t last_l = lasts[l];
  int32_t last_r = lasts[r];

  int32_t ll = std::min(last_l, last_r);
  int32_t lr = std::max(last_l, last_r);

  int32_t ca_depth = seg_tree_query(0, 0, depth_tree.size() / 2 - 1, ll, lr);

  int32_t l_depth = depths[last_l];
  int32_t r_depth = depths[last_r];
  return l_depth + r_depth - 2 * ca_depth;
}

int main() {
  stdin_input();
  stdout_output();

  int32_t n, q;

  std::cin >> n;
  edges = std::vector(n, V<int32_t>());
  while (--n) {
    int32_t u, v;
    std::cin >> u >> v;

    edges[u].emplace_back(v);
    edges[v].emplace_back(u);
  }

  tree_build();
  lca_setup();
  seg_tree_build();

  std::cin >> q;
  while (q--) {
    int32_t u, v, t;
    std::cin >> u >> v >> t;

    int32_t req = energy_required(u, v);

    if (req <= t) {
      std::cout << "Yes" << std::endl;
    } else {
      std::cout << "No" << std::endl;
    }
  }
}
