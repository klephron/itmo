#include "merge_bb.h"
#include "compiler/exception/list.h"

mir_merge_bb_result mir_merge_bb(mir *mir) {
  mir_merge_bb_result result = {
      .exceptions = list_exception_new(),
  };

  return result;
}
