#include <cstdint>
#include <iostream>
#include <list>
#include <vector>

// #include "helpers.hpp"

using LPII = typename std::list<std::pair<int32_t, int32_t>>;

template <typename T>
using V = typename std::vector<T>;

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

class Trie {
private:
  struct Node {
    Node* bits[2] = {0};
  } root;
  uint32_t size = 0;

public:
  ~Trie() {
    deleteNodes(root.bits[0]);
    deleteNodes(root.bits[1]);
  }

  void insert(uint32_t value) {
    bool inserted = false;
    Node* cur = &root;
    for (int i = (sizeof(value) * 8) - 1; i >= 0; --i) {
      int bit = (value >> i) & 1;

      Node*& next = cur->bits[bit];
      if (next == nullptr) {
        next = new Node();
        inserted = true;
      }

      cur = next;
    }
    if (inserted) {
      size += 1;
    }
  }

  uint32_t xorMax(uint32_t value) {
    Node* cur = &root;
    int32_t result = 0;
    for (int i = (sizeof(value) * 8) - 1; i >= 0; --i) {
      int bit = (value >> i) & 1;
      if (cur->bits[1 - bit]) {
        cur = cur->bits[1 - bit];
        result ^= ((1 - bit) << i);
      } else {
        cur = cur->bits[bit];
        result ^= (bit << i);
      }
    }
    return result;
  }

  void print() {
    std::cout << "size: " << size << std::endl;
    if (size != 0) {
      printNodes(&root, 0, 0);
    }
  }

private:
  void printNodes(Node* cur, int l, uint32_t prefix) {
    if (cur == nullptr) {
      return;
    }
    for (int i = 0; i < l; ++i) {
      std::cout << " ";
    }
    std::cout << cur;
    if (cur->bits[0] == nullptr && cur->bits[1] == nullptr) {
      std::cout << " - " << prefix << std::endl;
    } else {
      std::cout << std::endl;
    }
    printNodes(cur->bits[0], l + 1, (prefix << 1) | 0);
    printNodes(cur->bits[1], l + 1, (prefix << 1) | 1);
  }

  void deleteNodes(Node* cur) {
    if (cur == nullptr) {
      return;
    }
    deleteNodes(cur->bits[0]);
    deleteNodes(cur->bits[1]);
    delete cur;
  }
};

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

  uint32_t n, f;

  std::cin >> n >> f;
  --f;

  V<int32_t> root_xors(n, 0);
  V<bool> visited(n, 0);
  V<LPII> edges(n, LPII());
  Trie trie;

  int32_t u, v, w;
  for (uint32_t i = 0; i < n - 1; ++i) {
    std::cin >> u >> v >> w;
    --u, --v;
    edges[u].push_back(std::make_pair(v, w));
    edges[v].push_back(std::make_pair(u, w));
  }

  dfs(edges, root_xors, visited, 0);

  for (size_t i = 0; i < root_xors.size(); ++i) {
    if (i != f) {
      trie.insert(root_xors[i]);
    }
  }

  uint32_t xor_max = 0;
  for (uint32_t i = 1; i < n; ++i) {
    if (i != f) {
      xor_max = std::max(xor_max, trie.xorMax(root_xors[i]) ^ root_xors[i]);
    }
  }

  std::cout << xor_max << std::endl;

  return 0;
}
