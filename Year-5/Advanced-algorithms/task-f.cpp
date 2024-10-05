#include <iostream>
#include <string>
#include <vector>

template <typename T>
using V = typename std::vector<T>;

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

// #include "helpers.hpp"

int main() {
  stdin_input();
  stdout_output();

  size_t n, m;
  std::cin >> n >> m;

  std::string tape;
  std::cin >> tape;

  V<std::string> words(m);
  for (size_t i = 0; i < m; ++i) {
    std::cin >> words[i];
  }

  return 0;
}
