#include <climits>
#include <cmath>
#include <cstdio>
#include <iostream>
#include <vector>

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

template <typename T>
using V = typename std::vector<T>;

#define LCA_SIZE 200000
#define NODES_SIZE 100000
#define SPT_LOG 18

V<V<int32_t>> edges;
int32_t visited[NODES_SIZE];

V<V<int32_t>> tr_ch;
int32_t depths[LCA_SIZE];
int32_t nodes[LCA_SIZE];
int32_t lasts[NODES_SIZE];
int32_t sp_table[LCA_SIZE][SPT_LOG];

// returns next i
static int32_t lca_setup_dfs(int32_t d, int32_t i, int32_t p) {
  nodes[i] = p;
  depths[i] = d;
  for (auto ch : tr_ch[p]) {
    i = lca_setup_dfs(d + 1, i + 1, ch);
    nodes[i] = p;
    depths[i] = d;
  }
  lasts[p] = i;
  return i + 1;
}

static void sparse_table_min_build(int32_t n) {
  for (int32_t i = 0; i < n; ++i) {
    sp_table[i][0] = depths[i];
  }

  for (int32_t j = 1; j < SPT_LOG; ++j) {
    for (int32_t i = 0; i + (1 << j) - 1 < n; ++i) {
      sp_table[i][j] = std::min(sp_table[i][j - 1], sp_table[i + (1 << (j - 1))][j - 1]);
    }
  }
}

void tr_ch_build_dfs(int32_t p) {
  visited[p] = 1;
  for (auto c : edges[p]) {
    if (!visited[c]) {
      tr_ch[p].push_back(c);
      tr_ch_build_dfs(c);
    }
  }
}

void tr_ch_build() {
  int32_t n = edges.size();
  tr_ch = std::vector(n, V<int32_t>());

  for (int32_t i = 0; i < n; ++i) {
    visited[i] = 0;
  }
  tr_ch_build_dfs(0);
}

int main() {
  stdin_input();
  stdout_output();

  int32_t n, q;

  std::cin >> n;
  int32_t size = n;

  edges = std::vector(size, V<int32_t>());
  while (--n) {
    int32_t u, v;
    std::cin >> u >> v;

    edges[u].emplace_back(v);
    edges[v].emplace_back(u);
  }

  tr_ch_build();

  lca_setup_dfs(0, 0, 0);

  sparse_table_min_build(size * 2 - 1);

  std::cin >> q;
  while (q--) {
    int32_t a, b, t;
    std::cin >> a >> b >> t;

    int32_t ll = lasts[a] < lasts[b] ? lasts[a] : lasts[b];
    int32_t lr = lasts[a] > lasts[b] ? lasts[a] : lasts[b];

    int32_t size = lr - ll + 1;
    int32_t log = log2(size);
    int32_t sp_l = sp_table[ll][log];
    int32_t sp_r = sp_table[lr - (1 << log) + 1][log];
    int32_t ca_depth = std::min(sp_l, sp_r);

    int32_t l_depth = depths[lasts[a]];
    int32_t r_depth = depths[lasts[b]];

    if (l_depth + r_depth - 2 * ca_depth <= t) {
      std::cout << "Yes" << std::endl;
    } else {
      std::cout << "No" << std::endl;
    }
  }
}
