#pragma once

#include <cassert>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <list>
#include <stack>
#include <unordered_map>
#include <vector>

namespace youndex::express {

struct Item {
  std::uint64_t id;
  std::uint16_t score;
};

// adapted https://habr.com/ru/articles/150732/
struct AVLNode {
  Item* key;
  size_t size;  // children in subtree
  int height;   // can be stored in 1 byte (signed)
  AVLNode* left;
  AVLNode* right;

  AVLNode(Item* key) : key(key) {
    left = right = nullptr;
    height = 1;
    size = 1;
  }
};

struct AVLTree {
private:
  struct AVLNode* root;

public:
  AVLTree() : root(nullptr) {
  }

  ~AVLTree() {
    traverseDelete(root);
  }

  void insert(Item* item) {
    this->root = _insert(root, item);
  }
  void remove(Item* item) {
    this->root = _remove(root, item);
  }
  std::vector<uint64_t> kStLimitAtPosition(size_t k, size_t limit) const {
    return toVectorUint(_kstlimit(root, k, limit));
  }

  std::vector<uint64_t> kStLimitAtItem(Item* item, size_t limit) const {
    return toVectorUint(_kstlimit(root, item, limit));
  }

private:
  void traverseDelete(struct AVLNode* root) {
    if (root == nullptr) {
      return;
    }
    traverseDelete(root->left);
    traverseDelete(root->right);
    delete root;
  }

  std::vector<uint64_t> toVectorUint(std::vector<AVLNode*>&& nodes) const {
    std::vector<uint64_t> items;
    for (auto n : nodes) {
      items.emplace_back(n->key->id);
    }
    return items;
  }

private:
  int _cmp(const Item* lsv, const Item* rsv) const {
    if (lsv->score == rsv->score) {
      if (lsv->id > rsv->id) {
        return 1;
      } else if (lsv->id < rsv->id) {
        return -1;
      } else {
        return 0;
      }
    } else if (lsv->score > rsv->score) {
      return -1;
    } else {
      return 1;
    }
  }

  size_t _height(AVLNode* node) const {
    return node ? node->height : 0;
  }

  size_t _size(AVLNode* node) const {
    return node ? node->size : 0;
  }

  // signed
  int _bfactor(AVLNode* node) const {
    return _height(node->right) - _height(node->left);
  }

  void _fix(AVLNode* node) {
    size_t hl = _height(node->left);
    size_t hr = _height(node->right);
    node->height = (hl > hr ? hl : hr) + 1;
    size_t sl = _size(node->left);
    size_t sr = _size(node->right);
    node->size = sl + sr + 1;
  }

  AVLNode* _rotateright(AVLNode* p) {
    AVLNode* q = p->left;
    p->left = q->right;
    q->right = p;
    _fix(p);
    _fix(q);
    return q;
  }

  AVLNode* _rotateleft(AVLNode* q) {
    AVLNode* p = q->right;
    q->right = p->left;
    p->left = q;
    _fix(q);
    _fix(p);
    return p;
  }

  void print(AVLNode* p) const {
    std::cout << p->key->id << "+h:" << p->height;
    if (p->left) {
      std::cout << ",l:" << p->left->key->id << "+h:" << p->left->height;
    }
    if (p->right) {
      std::cout << ",r:" << p->right->key->id << "+h:" << p->right->height;
    }
    std::cout << std::endl;
  }

  AVLNode* _balance(AVLNode* p) {
    _fix(p);
    if (_bfactor(p) == 2) {
      if (_bfactor(p->right) < 0) {
        p->right = _rotateright(p->right);
      }
      return _rotateleft(p);
    }
    if (_bfactor(p) == -2) {
      if (_bfactor(p->left) > 0) {
        p->left = _rotateleft(p->left);
      }
      return _rotateright(p);
    }
    return p;
  }

  AVLNode* _insert(AVLNode* p, Item* k) {
    if (!p) {
      return new AVLNode(k);
    }

    int c = _cmp(k, p->key);
    if (c < 0) {
      p->left = _insert(p->left, k);
    } else {
      p->right = _insert(p->right, k);
    }
    return _balance(p);
  }

  AVLNode* _findmin(AVLNode* p) const {
    return p->left ? _findmin(p->left) : p;
  }

  AVLNode* _removemin(AVLNode* p) {
    if (p->left == nullptr) {
      return p->right;
    }
    p->left = _removemin(p->left);
    return _balance(p);
  }

  AVLNode* _remove(AVLNode* p, Item* k) {
    if (!p) {
      return 0;
    }

    int c = _cmp(k, p->key);
    if (c < 0) {
      p->left = _remove(p->left, k);
    } else if (c > 0) {
      p->right = _remove(p->right, k);
    } else {
      AVLNode* q = p->left;
      AVLNode* r = p->right;
      delete p;
      if (!r) {
        return q;
      }
      AVLNode* min = _findmin(r);
      min->right = _removemin(r);
      min->left = q;
      return _balance(min);
    }
    return _balance(p);
  }

  void _print_dfs(AVLNode* p, size_t depth) const {
    if (p == nullptr) {
      return;
    }
    _print_dfs(p->left, depth + 1);
    for (int i = 0; (size_t)i < depth; ++i) {
      std::cout << "  ";
    }
    std::cout << "id=" << p->key->id << ",score=" << p->key->score << ",size=" << p->size
              << std::endl;
    _print_dfs(p->right, depth + 1);
  }

  std::vector<AVLNode*> _kstlimit(AVLNode* p, size_t k_min, size_t limit) const {
    std::stack<AVLNode*> stack;

    // find k statistics + update stack
    size_t k = k_min + 1;
    if (p == nullptr || p->size < k) {
      return std::vector<AVLNode*>();
    }
    AVLNode* cur = p;
    while (k != 0) {
      if (cur->left) {
        if (k <= cur->left->size) {
          stack.push(cur);
          cur = cur->left;
        } else {
          k -= cur->left->size + 1;
          if (!k) {
            break;
          } else {
            cur = cur->right;
          }
        }
      } else {
        k -= 1;
        if (!k) {
          stack.push(cur);
          break;
        } else {
          cur = cur->right;
        }
      }
    }

    return _inorderFromStack(stack, limit);
  }

  // NOTE: element should exist
  std::vector<AVLNode*> _kstlimit(AVLNode* p, Item* item, size_t limit) const {
    if (p == nullptr) {
      return std::vector<AVLNode*>();
    }

    std::stack<AVLNode*> stack;

    AVLNode* cur = p;
    int c = _cmp(item, cur->key);

    if (!c) {
      stack.push(p);
    } else {
      while (!c) {
        if (c < 0) {
          stack.push(cur);
          cur = cur->left;
        } else {
          cur = cur->right;
        }
        if (cur == nullptr) {
          std::cout << "nullptr" << std::endl;
        }
        c = _cmp(item, cur->key);
      }
      stack.push(cur);
    }

    return _inorderFromStack(stack, limit);
  }

  std::vector<AVLNode*> _inorderFromStack(std::stack<AVLNode*>& stack, size_t limit) const {
    std::vector<AVLNode*> nodes;

    size_t k = 0;
    bool skipLeft = true;
    AVLNode* cur = stack.top();

    // in-order traverse starting from k-statistics
    while (k < limit && (cur != nullptr || !stack.empty())) {
      while (!skipLeft && cur != nullptr) {
        stack.push(cur);
        cur = cur->left;
      }
      skipLeft = false;

      // current
      cur = stack.top();
      nodes.push_back(cur);
      stack.pop();
      ++k;
      cur = cur->right;
    }
    return nodes;
  }
};

class ItemFeed final {
private:
  AVLTree avl;
  std::unordered_map<std::uint64_t, Item*> index;
  std::list<Item> items;

public:
  [[nodiscard]] std::vector<std::uint64_t> GetAtPosition(std::size_t position, std::size_t limit)
      const {
    return avl.kStLimitAtPosition(position, limit);
  }

  [[nodiscard]] std::vector<std::uint64_t> GetAtItemWithId(std::uint64_t uid, std::size_t limit)
      const {
    return avl.kStLimitAtItem(index.find(uid)->second, limit);
  }

  void Add(Item item) {
    items.push_back(item);
    Item* itemsItem = &items.back();
    avl.insert(itemsItem);
    index.insert({itemsItem->id, itemsItem});
  }

  void Update(Item item) {
    Item* itemsItem = index.find(item.id)->second;

    avl.remove(itemsItem);
    *itemsItem = item;
    avl.insert(itemsItem);
  }

  void Remove(std::uint64_t uid) {
    auto indexIt = index.find(uid);
    auto itemsIt = itemPtrToItemsIter(indexIt->second);

    avl.remove(indexIt->second);
    items.erase(itemsIt);
    index.erase(indexIt);
  }

private:
  [[nodiscard]] bool Contains(std::uint64_t uid) const {
    return index.find(uid) != index.end();
  }

  [[nodiscard]] std::size_t Size() const {
    return items.size();
  }

  std::list<Item>::iterator itemPtrToItemsIter(Item* ptr) {
    size_t iterOffset = (size_t) & (*((std::list<void*>::iterator) nullptr));
    std::list<Item>::iterator iter;
    *(intptr_t*)&iter = (intptr_t)ptr - iterOffset;
    return iter;
  }
};

}  // namespace youndex::express
