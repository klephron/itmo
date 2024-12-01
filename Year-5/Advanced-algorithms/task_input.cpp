#include "iostream"

#define stdout_output() (freopen("input_gen.txt", "w", stdout))

int main() {
  stdout_output();

  size_t n = 100000;
  std::cout << n << std::endl;
  for (size_t i = 0; i < n - 1; ++i) {
    std::cout << 0 << " " << i + 1 << std::endl;
  }
  size_t q = 1000000;
  std::cout << q << std::endl;
  for (size_t i = 0; i < q; ++i) {
    std::cout << i / n << " " << i % n << " " << q << std::endl;
  }
}
