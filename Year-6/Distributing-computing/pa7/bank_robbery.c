/*
 * Provided for testing purposes only!
 * In tests another implementation is used.
 */

#include "banking.h"

static void total_sum_snapshot() {
  // TODO: testing implementation
}

void bank_robbery(void *parent_data, local_id max_id) {
  // TODO: testing implementation
  for (int i = 1; i < max_id; ++i) {
    transfer(parent_data, i, i + 1, i);
    if (i % 2) {
      total_sum_snapshot();
    }
  }
  if (max_id > 1) {
    transfer(parent_data, max_id, 1, 1);
    total_sum_snapshot();
  }
}

timestamp_t get_vector_time() {
  // TODO: testing implementation
  return 0;
}
