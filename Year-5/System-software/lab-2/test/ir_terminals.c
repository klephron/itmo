#include "ir/terminals.h"
#include <criterion/criterion.h>

Test(ir_terminals, id) {
  ir_term_id *id = ir_term_id_new("string");
  ir_term_id_free(id);
}
