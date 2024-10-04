#include <iostream>
#include <list>
#include <vector>

// #include "helpers.hpp"

using LPII = typename std::list<std::pair<int32_t, int32_t>>;

template <typename T>
using V = typename std::vector<T>;

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

void dfs(const V<LPII>& edges, V<int32_t>& root_xors, V<bool>& visited, const int32_t i) {
  visited[i] = 1;
  for (const auto& [j, w] : edges[i]) {
    if (visited[j] == false) {
      root_xors[j] = root_xors[i] ^ w;
      dfs(edges, root_xors, visited, j);
    }
  }
}

int main() {
  stdin_input();
  stdout_output();

  int32_t n, f;

  std::cin >> n >> f;
  --f;

  V<int32_t> root_xors(n, 0);
  V<bool> visited(n, 0);
  V<LPII> edges(n, LPII());

  int32_t u, v, w;
  for (int32_t i = 0; i < n - 1; ++i) {
    std::cin >> u >> v >> w;
    --u, --v;
    edges[u].push_back(std::make_pair(v, w));
    edges[v].push_back(std::make_pair(u, w));
  }

  // calculate xors from root node
  dfs(edges, root_xors, visited, 0);

  int32_t xor_max = 0;

  for (int32_t i = 0; i < n; ++i) {
    if (i != f) {
      for (int32_t j = i + 1; j < n; ++j) {
        if (j != f) {
          xor_max = std::max(xor_max, root_xors[i] ^ root_xors[j]);
        }
      }
    }
  }

  std::cout << xor_max << std::endl;

  return 0;
}
