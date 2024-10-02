#pragma once

#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <vector>

#include "helpers.hpp"

namespace youndex::express {

struct Item {
  std::uint64_t id;
  std::uint16_t score;
};

class ItemFeed final {
private:


public:
  ItemFeed() {
    println("constructor");
  }

  ~ItemFeed() {
    println("destructor");
  }

  [[nodiscard]] std::vector<std::uint64_t> GetAtPosition(std::size_t position, std::size_t limit)
      const {
  }

  [[nodiscard]] std::vector<std::uint64_t> GetAtItemWithId(std::uint64_t uid, std::size_t limit)
      const {
  }

  void Add(Item item) {
  }

  void Update(Item item) {
  }

  void Remove(std::uint64_t uid) {
  }

private:
  [[nodiscard]] bool Contains(std::uint64_t uid) const {
  }

  [[nodiscard]] std::size_t Size() const {
  }
};

}  // namespace youndex::express
