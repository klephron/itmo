#include <criterion/criterion.h>
#include <stdio.h>

#include "util/container_util.h"
#include "util/hashset.h"
#include "util/log.h"

Test(hashset, create_delete) {
  p_hashset hashset =
      hashset_new(container_cmp_chars_chars, container_new_chars_chars,
                  container_delete_chars_chars, container_hash_chars_chars);

  hashset_free(hashset);
}

Test(hashset, insert) {
  p_hashset hashset =
      hashset_new(container_cmp_chars_chars, container_new_chars_chars,
                  container_delete_chars_chars, container_hash_chars_chars);

  char key[10];
  char value[10];

  chars_chars data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    hashset_insert(hashset, &data);
  }

  hashset_free(hashset);
}

Test(hashset, iterate_1000) {
  p_hashset hashset =
      hashset_new(container_cmp_chars_chars, container_new_chars_chars,
                  container_delete_chars_chars, container_hash_chars_chars);

  char key[10];
  char value[10];

  chars_chars data = {.key = key, .value = value};

  for (int i = 0; i < 1000; ++i) {
    sprintf(key, "%d", i);
    sprintf(value, "%d", i + 1000);
    hashset_insert(hashset, &data);
  }

  size_t cnt = 0;
  for (hashset_it it = hashset_begin(hashset); !it.end(&it); it.next(&it)) {
    cnt += 1;
  }

  cr_expect_eq(1000, cnt);

  hashset_free(hashset);
}

Test(hashset, iterate_empty) {
  p_hashset hashset =
      hashset_new(container_cmp_chars_chars, container_new_chars_chars,
                  container_delete_chars_chars, container_hash_chars_chars);

  size_t cnt = 0;
  for (hashset_it it = hashset_begin(hashset); !it.end(&it); it.next(&it)) {
    it.get(&it);
    cnt += 1;
  }

  cr_expect_eq(0, cnt);

  hashset_free(hashset);
}
