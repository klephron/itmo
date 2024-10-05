#include <cstdint>
#include <iostream>
#include <string>
#include <vector>

template <typename T>
using V = typename std::vector<T>;

#define stdin_input() (freopen("input.txt", "r", stdin))
#define stdout_output() (freopen("output.txt", "w", stdout))

// #include "helpers.hpp"

V<size_t> prefix(const std::string& word) {
  const size_t n = word.size();
  V<size_t> p(n, 0);
  for (size_t i = 1; i < n; ++i) {
    size_t k = p[i - 1];
    while ((k > 0) && (word[i] != word[k])) {
      k = p[k - 1];
    }
    if (word[i] == word[k]) {
      ++k;
    }
    p[i] = k;
  }
  return p;
}

// if not found -> return size_max
// start - starting pos
size_t find_first_kmp(const std::string& tape, size_t start, const std::string& word) {
  size_t result = SIZE_MAX;
  auto p = prefix(word);

  size_t n = tape.size();
  size_t m = word.size();
  size_t i = start;
  size_t j = 0;

  if (start >= n) {
    return result;
  }

  while ((n - i) >= (m - j)) {
    if (word[j] == tape[i]) {
      j++;
      i++;
    }
    if (j == m) {
      result = i - j;
      break;
    } else if (i < n && word[j] != tape[i]) {
      if (j != 0) {
        j = p[j - 1];
      } else {
        i++;
      }
    }
  }

  return result;
}

int main() {
  stdin_input();
  stdout_output();

  size_t n, m;
  std::cin >> n >> m;

  std::string tape;
  std::cin >> tape;

  std::string word;
  size_t start = 0;
  bool result = true;

  for (size_t i = 0; i < m; ++i) {
    std::cin >> word;
    start = find_first_kmp(tape, start, word);
    if (start == SIZE_MAX) {
      result = false;
      break;
    }
    start += word.size() + 1;
  }

  if (result) {
    std::cout << "YES" << std::endl;
  } else {
    std::cout << "NO" << std::endl;
  }

  return 0;
}

/*
 * Z or prefix function:
 *
 * ababacabca
 * aba
 * ca
 *
 * [aba]bacabca -> bacabca
 * ab[aba]cabca -> cabca
 *
 * do greedy: after each deletion of word string will be longest
 */
