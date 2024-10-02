#include "task-c.hpp"

#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <ostream>

namespace youndex::express::test {

void Print(std::ostream& out, std::string_view name, const std::vector<std::uint64_t>& ids) {
  out << name << ": [";
  for (const auto uid : ids) {
    out << uid << ", ";
  }
  out << "]";
}

void Test(std::istream& in, std::ostream& out) {
  ItemFeed feed;

  std::size_t actions_count = 0;
  in >> actions_count;
  out << "Started" << "\n";

  for (std::size_t action = 0; action < actions_count; ++action) {
    char command = 0;
    in >> command;

    switch (command) {
      case 'i': {  // i <id> <score>
        std::uint64_t uid = 0;
        std::uint16_t score = 0;
        in >> uid >> score;

        feed.Add({.id = uid, .score = score});
        out << "Added" << "\n";
      } break;
      case 'u': {
        std::uint64_t uid = 0;
        std::uint16_t score = 0;
        in >> uid >> score;

        feed.Update({.id = uid, .score = score});
        out << "Updated" << "\n";
      } break;
      case 'r': {  // r <id>
        std::uint64_t uid = 0;
        in >> uid;

        feed.Remove(uid);
        out << "Removed" << "\n";
      } break;
      case 'p': {  // p <pos> <limit>
        std::uint64_t pos = 0;
        std::size_t limit = 0;
        in >> pos >> limit;

        Print(out, "AtPos", feed.GetAtPosition(pos, limit));
        out << "\n";
      } break;
      case 'g': {  // g <id> <limit>
        std::uint64_t uid = 0;
        std::size_t limit = 0;
        in >> uid >> limit;

        Print(out, "AtId", feed.GetAtItemWithId(uid, limit));
        out << "\n";
      } break;
      default: {
        assert(false);
      } break;
    }
  }
}

void Main() {
  std::ifstream ifstream;
  std::ofstream ofstream;

  ifstream.open("input.txt");
  ofstream.open("output.txt");

  Test(ifstream, ofstream);

  ofstream.close();
  ifstream.close();
}

}  // namespace youndex::express::test

int main() {
  youndex::express::test::Main();
}
