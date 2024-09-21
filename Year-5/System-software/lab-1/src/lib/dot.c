#include "dot.h"

#include <antlr3.h>

dot_string dot_ast(pANTLR3_BASE_TREE tree) {

  pANTLR3_STRING string = tree->toStringTree(tree);

  return (dot_string){.chars = (const char *)string->chars, .len = string->len};
}
