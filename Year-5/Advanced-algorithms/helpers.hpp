#pragma once

#include <cstddef>
#include <iostream>

template <typename T>
void print_array(const T *arr, size_t size, const char *arr_name) {
  if (arr_name) {
    std::cout << arr_name << ": ";
  } else {
    std::cout << "arr: ";
  }
  for (size_t i = 0; i < size; ++i) {
    std::cout << "[" << i << "]=" << arr[i] << ", ";
  }
  std::cout << std::endl;
}

template <typename T>
void print_array(const T *arr, size_t size) {
  print_array(arr, size, NULL);
}

template <typename T>
void println(const T v, const char *name) {
  if (name) {
    std::cout << name << "=" << v << std::endl;
  } else {
    std::cout << "value=" << v << std::endl;
  }
}

template <typename T>
void println(const T v) {
  println(v, NULL);
}

template <typename T>
void print(const T v, const char *name) {
  if (name) {
    std::cout << name << "=" << v << ", ";
  } else {
    std::cout << "value=" << v << ", ";
  }
}

template <typename T>
void print(const T v) {
  print(v, NULL);
}
