#include <climits>
#include <cstdio>
#include <iostream>
#include <list>
#include <tuple>
#include <vector>

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

template <typename T>
using V = typename std::vector<T>;

template <typename T>
using L = typename std::list<T>;

static inline long seg_tree_min_i_val(const V<long>& arr, long left_i, long right_i) {
  if (left_i == LONG_MAX) {
    return right_i;
  }
  if (right_i == LONG_MAX) {
    return left_i;
  }
  return arr[left_i] < arr[right_i] ? left_i : right_i;
}

static long seg_tree_min_i_query_int(
    const V<long>& tree, const V<long>& arr, long i, long tl, long tr, const long l, const long r
) {
  if (l <= tl && tr <= r) {
    return tree[i];
  } else if (tr < l || tl > r) {
    return LONG_MAX;
  } else {
    long mid = tl + (tr - tl) / 2;
    long left_i = seg_tree_min_i_query_int(tree, arr, 2 * i + 1, tl, mid, l, r);
    long right_i = seg_tree_min_i_query_int(tree, arr, 2 * i + 2, mid + 1, tr, l, r);
    return seg_tree_min_i_val(arr, left_i, right_i);
  }
}

static inline long seg_tree_min_i_query(const V<long>& tree, const V<long>& arr, long a, long b) {
  return seg_tree_min_i_query_int(tree, arr, 0, 0, tree.size() / 2 - 1, a, b);
}

static V<long> segment_tree_min_i_create(const V<long>& arr) {
  size_t n = arr.size();

  // get tree size
  size_t tr_sz = 1;
  while (tr_sz < n) {
    tr_sz *= 2;
  }
  tr_sz *= 2;

  // build tree
  V<long> tree = V<long>(tr_sz);

  size_t ti = (tr_sz / 2 - 1);
  for (size_t i = 0; i < n; ++i, ++ti) {
    tree[ti] = i;
  }
  for (; ti < tr_sz; ++ti) {
    tree[ti] = LONG_MAX;
  }
  for (ti = (tr_sz / 2) - 2;; --ti) {
    long left_i = tree[2 * ti + 1];
    long right_i = tree[2 * ti + 2];
    tree[ti] = seg_tree_min_i_val(arr, left_i, right_i);
    if (ti == 0)
      break;
  }

  return tree;
}

// returns next i
static size_t lca_setup_dfs(
    V<L<long>>& tr_ch, V<long>& depths, V<long>& nodes, V<long>& lasts, size_t d, size_t i, size_t p
) {
  nodes[i] = p;
  depths[i] = d;
  for (auto ch : tr_ch[p]) {
    i = lca_setup_dfs(tr_ch, depths, nodes, lasts, d + 1, i + 1, ch);
    nodes[i] = p;
    depths[i] = d;
  }
  lasts[p] = i;
  return i + 1;
}

static std::tuple<V<long>, V<long>, V<long>> lca_setup(V<L<long>>& tr_ch) {
  size_t n = tr_ch.size();
  V<long> nodes = V<long>(n * 2 - 1);
  V<long> depths = V<long>(n * 2 - 1);
  V<long> lasts = V<long>(n);

  lca_setup_dfs(tr_ch, depths, nodes, lasts, 0, 0, 0);

  return std::make_tuple(nodes, depths, lasts);
}

static long lca(
    const V<long>& nodes,
    const V<long>& depths,
    const V<long>& lasts,
    const V<long>& depth_tree,
    long a,
    long b
) {
  size_t l = std::min(lasts[a], lasts[b]);
  size_t r = std::max(lasts[a], lasts[b]);

  long min_i = seg_tree_min_i_query(depth_tree, depths, l, r);
  return nodes[min_i];
}

long energy_required(
    V<long>& nodes, V<long>& depths, V<long>& lasts, V<long>& depth_tree, long l, long r
) {
  long ca = lca(nodes, depths, lasts, depth_tree, l, r);
  long ca_depth = depths[lasts[ca]];
  long l_depth = depths[lasts[l]];
  long r_depth = depths[lasts[r]];
  return l_depth + r_depth - 2 * ca_depth;
}

void build_tree_i(V<L<long>>& edges, V<long>& visited, V<L<long>>& tr_ch, long p) {
  visited[p] = 1;
  for (auto c : edges[p]) {
    if (!visited[c]) {
      tr_ch[p].push_back(c);
      build_tree_i(edges, visited, tr_ch, c);
    }
  }
}

V<L<long>> build_tree(V<L<long>>& edges) {
  size_t n = edges.size();
  V<L<long>> tr_ch = std::vector(n, L<long>());
  V<long> visited = std::vector<long>(n, 0);
  build_tree_i(edges, visited, tr_ch, 0);
  return tr_ch;
}

int main() {
  stdin_input();
  stdout_output();

  long n, q;

  std::cin >> n;
  V<L<long>> edges = std::vector(n, L<long>());
  while (--n) {
    long u, v;
    std::cin >> u >> v;

    edges[u].push_back(v);
    edges[v].push_back(u);
  }

  V<L<long>> tr_ch = build_tree(edges);

  // std::cout << "children: " << std::endl;
  // for (size_t i = 0; i < tr_ch.size(); ++i) {
  //   std::cout << i << ": ";
  //   for (auto e : tr_ch[i]) {
  //     std::cout << e << ", ";
  //   }
  //   std::cout << std::endl;
  // }

  auto [nodes, depths, lasts] = lca_setup(tr_ch);
  auto depth_tree = segment_tree_min_i_create(depths);

  // std::cout << "nodes: " << std::endl;
  // for (auto e : nodes) {
  //   std::cout << e << " ";
  // }
  // std::cout << std::endl;

  // std::cout << "depths: " << std::endl;
  // for (auto e : depths) {
  //   std::cout << e << " ";
  // }
  // std::cout << std::endl;

  // std::cout << "lasts: " << std::endl;
  // for (auto e : lasts) {
  //   std::cout << e << " ";
  // }
  // std::cout << std::endl;

  // std::cout << "depth_tree: " << std::endl;
  // for (auto e : depth_tree) {
  //   std::cout << e << " ";
  // }
  // std::cout << std::endl;

  std::cin >> q;
  while (q--) {
    long u, v, t;
    std::cin >> u >> v >> t;

    long req = energy_required(nodes, depths, lasts, depth_tree, u, v);
    bool ok = (req <= t);

    if (ok) {
      std::cout << "Yes" << std::endl;
    } else {
      std::cout << "No" << std::endl;
    }
  }
}
