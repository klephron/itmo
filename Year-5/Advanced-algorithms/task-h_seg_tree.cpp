#include <climits>
#include <cstdio>
#include <iostream>
#include <vector>

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

template <typename T>
using V = typename std::vector<T>;

const size_t LCA_SIZE = 200000;
const size_t NODES_SIZE = 100000;
const size_t TREE_SIZE = 1 << 19;

V<V<int32_t>> edges;
int32_t visited[NODES_SIZE];

V<V<int32_t>> tree_children;
int32_t depths[LCA_SIZE];
int32_t lasts[NODES_SIZE];

int32_t depth_tree[TREE_SIZE];

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

static inline void seg_tree_build(size_t n) {
  size_t ti = (TREE_SIZE / 2 - 1);
  for (size_t i = 0; i < n; ++i, ++ti) {
    depth_tree[ti] = depths[i];
  }
  for (; ti < TREE_SIZE; ++ti) {
    depth_tree[ti] = INT_MAX;
  }
  for (ti = (TREE_SIZE / 2) - 2;; --ti) {
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

static inline void children_build_dfs(int32_t p) {
  visited[p] = 1;
  for (auto c : edges[p]) {
    if (!visited[c]) {
      tree_children[p].push_back(c);
      children_build_dfs(c);
    }
  }
}

static inline void children_build() {
  size_t n = edges.size();
  tree_children = std::vector<V<int32_t>>(n);

  for (size_t i = 0; i < NODES_SIZE; ++i) {
    visited[i] = 0;
  }

  children_build_dfs(0);
}

static inline int32_t energy_required(int32_t l, int32_t r) {
  int32_t last_l = lasts[l];
  int32_t last_r = lasts[r];

  int32_t ll = std::min(last_l, last_r);
  int32_t lr = std::max(last_l, last_r);

  int32_t ca_depth = seg_tree_query(0, 0, TREE_SIZE / 2 - 1, ll, lr);

  int32_t l_depth = depths[last_l];
  int32_t r_depth = depths[last_r];
  return l_depth + r_depth - 2 * ca_depth;
}

int main() {
  stdin_input();
  // stdout_output();
  std::ios_base::sync_with_stdio(false);
  std::cin.tie(nullptr);

  int32_t n, q;

  std::cin >> n;
  int32_t size = n;

  edges = std::vector<V<int32_t>>(n);
  while (--n) {
    int32_t u, v;
    std::cin >> u >> v;

    edges[u].emplace_back(v);
    edges[v].emplace_back(u);
  }

  children_build();
  lca_setup_dfs(0, 0, 0);
  seg_tree_build(size * 2 - 1);

  std::cin >> q;
  while (q--) {
    int32_t u, v, t;
    std::cin >> u >> v >> t;

    int32_t req = energy_required(u, v);

    if (req <= t) {
      std::cout << "Yes\n";
    } else {
      std::cout << "No\n";
    }
  }
}
