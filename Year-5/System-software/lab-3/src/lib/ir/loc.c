#include "ir/loc.h"

#include <antlr3commontree.h>
#include <antlr3parsetree.h>

ir_loc ir_loc_antlr3(pANTLR3_BASE_TREE tree) {
  pANTLR3_COMMON_TOKEN token = tree->getToken(tree);
  if (token) {
    return (ir_loc){.complete = 1,
                    .line = token->line,
                    .pos_start = token->getCharPositionInLine(token),
                    .pos_end = token->getCharPositionInLine(token) +
                               (token->stop - token->start + 1)};
  } else {
    return (ir_loc){.complete = 0};
  }
}
