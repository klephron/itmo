#include "pass2.h"
#include "exception.h"
#include "ir/ctx.h"
#include "ir/scope.h"
#include "util/container_util.h"
#include "util/log.h"
#include "util/macro.h"
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static long ir_pass2_integer(const char *str, int base, char **err_str_out) {
  char errbuf[256];
  errbuf[0] = '\0';

  errno = 0;
  char *endptr = NULL;
  long  number = strtol(str, &endptr, base);

  /* test return to number and errno values */
  if (str == endptr)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (no digits found, 0 returned)\n", number);
  else if (errno == ERANGE && number == LONG_MIN)
    snprintf(errbuf, sizeof(errbuf) - 1, "%lu invalid (underflow occurred)",
             number);
  else if (errno == ERANGE && number == LONG_MAX)
    snprintf(errbuf, sizeof(errbuf) - 1, "%lu invalid (overflow occurred)",
             number);
  else if (errno == EINVAL) /* not in all c99 implementations - gcc OK */
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (base contains unsupported value)", number);
  else if (errno != 0 && number == 0)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu invalid (unspecified error occurred)", number);
  else if (errno == 0 && str && *endptr != 0)
    snprintf(errbuf, sizeof(errbuf) - 1,
             "%lu valid (but additional characters remain)", number);

  if (errbuf[0] != '\0') {
    *err_str_out = malloc(sizeof(errbuf));
    memcpy(*err_str_out, errbuf, sizeof(errbuf));
  }

  return number;
}

void ir_pass2_init(ir_pass2 *self, struct ir_ctx *ctx) {
  self->base.kind = IR_PASS_2;
  self->ctx = ctx;

  self->scope_stack = ir_scope_stack_new();
  ir_scope *global = ir_scope_new(IR_SCOPE_GLOBAL, NULL, NULL, NULL);
  ir_scope_stack_push(self->scope_stack, global);
  // insert subroutines in global scope
  for (hashset_it it = hashset_begin(ctx->subroutines); !it.end(&it);
       it.next(&it)) {
    ir_subroutine *sub = it.get(&it);
    ir_scope_symbol_add_sub(global, sub);
  }

  self->break_stack = NULL;
  self->cfg = NULL;
}

void ir_pass2_deinit(ir_pass2 *self) {
  ir_scope *scope = ir_scope_stack_pop(self->scope_stack);
  if (scope->type != IR_SCOPE_GLOBAL) {
    error("expected global scope when deiniting");
  }
  ir_scope_free(scope);

  ir_scope_stack_free(self->scope_stack);
}

ir_term_lit *ir_pass2_str(ir_pass2 *self, ir_loc loc, const char *str) {
  debug("pass2 creating str %s", str);
  UNUSED(self);

  ir_term_lit_v u;
  u.v_str = strdup(str);
  return ir_term_lit_new(loc, IR_T_STRING, u);
}

ir_term_lit *ir_pass2_rune(ir_pass2 *self, ir_loc loc, const char *str) {
  debug("pass2 creating rune %s", str);
  // NOTE: checked by lexer
  if (strlen(str) != 3 && str[0] != '\'' && str[2] != '\'') {
    const int  subtype = EXCEPTION_IR_CHAR_VALIDATION;
    exception *ex = exception_new_f(
        EXCEPTION_IR, subtype, self->file_name, loc.line, loc.pos_start,
        exception_subtype_ir_str(subtype), "char %s not valid", str);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }
  ir_term_lit_v u;
  u.v_char = str[1];
  return ir_term_lit_new(loc, IR_T_CHAR, u);
}

ir_term_lit *ir_pass2_hex(ir_pass2 *self, ir_loc loc, const char *str) {
  UNUSED(self);
  debug("pass2 creating hex %s", str);

  ir_term_lit_v u;
  char         *errstr = NULL;
  u.v_int = ir_pass2_integer(str + 2, 16, &errstr);

  if (errstr) {
    const int  subtype = EXCEPTION_IR_HEX_VALIDATION;
    exception *ex = exception_new_f(EXCEPTION_IR, subtype, self->file_name,
                                    loc.line, loc.pos_start,
                                    exception_subtype_ir_str(subtype), errstr);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 hex=%d", u.v_int);

  // TODO: calculate optimal type
  return ir_term_lit_new(loc, IR_T_LONG, u);
}

ir_term_lit *ir_pass2_bits(ir_pass2 *self, ir_loc loc, const char *str) {
  UNUSED(self);
  debug("pass2 creating bits %s", str);

  ir_term_lit_v u;
  char         *errstr = NULL;
  u.v_int = ir_pass2_integer(str + 2, 2, &errstr);

  if (errstr) {
    const int  subtype = EXCEPTION_IR_BITS_VALIDATION;
    exception *ex = exception_new_f(EXCEPTION_IR, subtype, self->file_name,
                                    loc.line, loc.pos_start,
                                    exception_subtype_ir_str(subtype), errstr);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 bits=%d", u.v_int);

  // TODO: calculate optimal type
  return ir_term_lit_new(loc, IR_T_LONG, u);
}

ir_term_lit *ir_pass2_dec(ir_pass2 *self, ir_loc loc, const char *str) {
  UNUSED(self);
  debug("pass2 creating dec %s", str);

  ir_term_lit_v u;
  char         *errstr = NULL;
  u.v_int = ir_pass2_integer(str, 10, &errstr);

  if (errstr) {
    const int  subtype = EXCEPTION_IR_DEC_VALIDATION;
    exception *ex = exception_new_f(EXCEPTION_IR, subtype, self->file_name,
                                    loc.line, loc.pos_start,
                                    exception_subtype_ir_str(subtype), errstr);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 dec=%d", u.v_int);

  // TODO: calculate optimal type
  return ir_term_lit_new(loc, IR_T_LONG, u);
}

ir_term_lit *ir_pass2_bool(ir_pass2 *self, ir_loc loc, const char *str) {
  UNUSED(self);
  debug("pass2 creating bool %s", str);

  ir_term_lit_v u;

  if (!strcmp("true", str)) {
    u.v_bool = 1;
  } else if (!strcmp("false", str)) {
    u.v_bool = 0;
  } else {
    const int  subtype = EXCEPTION_IR_BOOL_VALIDATION;
    exception *ex = exception_new_f(
        EXCEPTION_IR, subtype, self->file_name, loc.line, loc.pos_start,
        exception_subtype_ir_str(subtype), "%s is invalid", str);
    ir_ctx_add_exception(self->ctx, ex);
    return NULL;
  }

  debug("pass2 bool=%d", u.v_bool);

  return ir_term_lit_new(loc, IR_T_BOOL, u);
}

ir_term_id *ir_pass2_term_id_new(ir_pass2 *self, ir_loc loc, const char *id) {
  UNUSED(self);
  return ir_term_id_new(loc, id);
}

// TODO: types
ir_expr_base *ir_pass2_expr_id_new(ir_pass2 *self, const ir_term_id *id) {
  UNUSED(self);
  if (!ir_scope_stack_find_symbol(self->scope_stack, id)) {
    const int  subtype = EXCEPTION_IR_SYMBOL_UNDEFINED;
    exception *ex = exception_new_f(EXCEPTION_IR, subtype, self->file_name,
                                    id->base.loc.line, id->base.loc.pos_start,
                                    exception_subtype_ir_str(subtype),
                                    "symbol %s is undefined", id->name);
    ir_ctx_add_exception(self->ctx, ex);
  }
  return (ir_expr_base *)ir_expr_id_new(id);
}

// TODO: types
ir_expr_base *ir_pass2_expr_lit_new(ir_pass2 *self, ir_term_lit *lit) {
  UNUSED(self);
  return (ir_expr_base *)ir_expr_lit_new(lit);
}

// TODO: types
ir_expr_base *ir_pass2_expr_index_new(ir_pass2 *self, ir_expr_base *indexed,
                                      p_list args) {
  UNUSED(self);
  return (ir_expr_base *)ir_expr_index_new(indexed, args);
}

// TODO: types
ir_expr_base *ir_pass2_expr_call_new(ir_pass2 *self, ir_expr_base *callee,
                                     p_list args) {
  UNUSED(self);
  return (ir_expr_base *)ir_expr_call_new(callee, args);
}

// TODO: types
ir_expr_base *ir_pass2_expr_unary_new(ir_pass2 *self, ir_expr_base *first,
                                      ir_expr_unary_enum op, ir_loc op_loc) {
  debug("creating unary expression with op=%d", op);

  switch (op) {
  case IR_EXPR_UNARY_DEC:
  case IR_EXPR_UNARY_INC: {
    if (!first) {
      const int  subtype = EXCEPTION_IR_NULL;
      exception *ex = exception_new_f(
          EXCEPTION_IR, subtype, self->file_name, op_loc.line, op_loc.pos_start,
          exception_subtype_ir_str(subtype), "unary expression is <null>");
      ir_ctx_add_exception(self->ctx, ex);
      goto error;
    }
    if (first->kind != IR_EXPR_IDENTIFIER) {
      const int  subtype = EXCEPTION_IR_NULL;
      exception *ex = exception_new_f(
          EXCEPTION_IR, subtype, self->file_name, op_loc.line, op_loc.pos_start,
          exception_subtype_ir_str(subtype), "cannot assign to rvalue");
      ir_ctx_add_exception(self->ctx, ex);
      goto error;
    }

    ir_term_lit_v u;
    u.v_int = 1;

    ir_expr_base *second;
    if (op == IR_EXPR_UNARY_INC) {
      second = (ir_expr_base *)ir_expr_binary_new(
          (ir_expr_base *)ir_expr_unary_new(first, IR_EXPR_UNARY_LOAD, op_loc),
          (ir_expr_base *)ir_expr_lit_new(
              ir_term_lit_new((ir_loc){}, IR_T_LONG, u)),
          IR_EXPR_BINARY_ADD, op_loc);
    } else {
      second = (ir_expr_base *)ir_expr_binary_new(
          (ir_expr_base *)ir_expr_unary_new(first, IR_EXPR_UNARY_LOAD, op_loc),
          (ir_expr_base *)ir_expr_lit_new(
              ir_term_lit_new((ir_loc){}, IR_T_LONG, u)),
          IR_EXPR_BINARY_SUB, op_loc);
    }
    return (ir_expr_base *)ir_expr_binary_new(ir_expr_copy(first), second,
                                              IR_EXPR_BINARY_ASSIGN, op_loc);
  }
  case IR_EXPR_UNKNOWN:
    error("unknown unary expression type=%d", op);
    goto error;
  default:
    return (ir_expr_base *)ir_expr_unary_new(first, op, op_loc);
  }

error:
  ir_expr_free(first);
  return NULL;
}

ir_expr_base *ir_pass2_expr_binary_new(ir_pass2 *self, ir_expr_base *first,
                                       ir_expr_base       *second,
                                       ir_expr_binary_enum op, ir_loc op_loc) {
  UNUSED(self);

  debug("creating binary expression type=%d", op);

  if (second->kind == IR_EXPR_IDENTIFIER) {
    second =
        (ir_expr_base *)ir_expr_unary_new(second, IR_EXPR_UNARY_LOAD, op_loc);
  }

  switch (op) {
  case IR_EXPR_BINARY_UNKNOWN:
    error("unknown binary expression type=%d", op);
    goto error;
  case IR_EXPR_BINARY_ASSIGN: {
    if (!first) {
      const int  subtype = EXCEPTION_IR_NULL;
      exception *ex = exception_new_f(
          EXCEPTION_IR, subtype, self->file_name, op_loc.line, op_loc.pos_start,
          exception_subtype_ir_str(subtype), "assigning value to <null>");
      ir_ctx_add_exception(self->ctx, ex);
      goto error;
    }
    if (first->kind != IR_EXPR_IDENTIFIER) {
      const int  subtype = EXCEPTION_IR_RVALUE_ASSIGNMENT;
      exception *ex = exception_new_f(
          EXCEPTION_IR, subtype, self->file_name, op_loc.line, op_loc.pos_start,
          exception_subtype_ir_str(subtype), "cannot assign to rvalue");
      ir_ctx_add_exception(self->ctx, ex);
      goto error;
    }
    break;
  }
  default:
    break;
  }

  return (ir_expr_base *)ir_expr_binary_new(first, second, op, op_loc);

error:
  ir_expr_free(first);
  ir_expr_free(second);
  return NULL;
}

ir_t_base *ir_pass2_t_bool_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_BOOL);
}
ir_t_base *ir_pass2_t_byte_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_BYTE);
}
ir_t_base *ir_pass2_t_int_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_INT);
}
ir_t_base *ir_pass2_t_uint_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_UINT);
}
ir_t_base *ir_pass2_t_long_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_LONG);
}
ir_t_base *ir_pass2_t_ulong_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_ULONG);
}
ir_t_base *ir_pass2_t_char_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_CHAR);
}
ir_t_base *ir_pass2_t_string_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_t_base_new(IR_T_STRING);
}
ir_t_base *ir_pass2_t_custom_new(ir_pass2 *self, ir_term_id *id) {
  UNUSED(self);
  ir_t_base *r = (ir_t_base *)ir_t_custom_new(id->name);
  ir_term_id_free(id);
  return r;
}
ir_t_base *ir_pass2_t_array_new(ir_pass2 *self, size_t dimension,
                                ir_t_base *type) {
  UNUSED(self);
  return (ir_t_base *)ir_t_array_new(dimension, type);
}

ir_var *ir_pass2_param_new(ir_pass2 *self, ir_term_id *id, ir_t_base *type) {
  UNUSED(self);
  return ir_var_new(id, type);
}

p_list ir_pass2_params_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_var_list_new();
}

int ir_pass2_param_add(ir_pass2 *self, p_list list, ir_var *var) {
  UNUSED(self);
  list_push_back(list, var);
  return 1;
}

ir_signature *ir_pass2_signature_new(ir_pass2 *self, ir_term_id *id,
                                     p_list vars, ir_t_base *ret) {
  UNUSED(self);
  return ir_signature_new(id, vars, ret);
}

p_list ir_pass2_ids_new(ir_pass2 *self) {
  UNUSED(self);
  return list_new(container_cmp_false, container_new_move,
                  container_delete_false);
}

void ir_pass2_id_add(ir_pass2 *self, p_list list, ir_term_id *id) {
  UNUSED(self);
  list_push_back(list, id);
}

p_list ir_pass2_var_new(ir_pass2 *self, p_list ids, ir_t_base *type) {
  UNUSED(self);
  p_list vars =
      list_new(container_cmp_false, container_new_move, container_delete_false);
  if (ids->head) {
    ir_var *first = ir_var_new(ids->head->data, type);
    list_push_back(vars, first);

    for (list_it it = ids->head->next; it; it = it->next) {
      ir_var *var = ir_var_new(it->data, ir_t_copy(type));
      list_push_back(vars, var);
    }
  } else {
    ir_t_free(type);
  }
  list_free(ids);
  return vars;
}

p_list ir_pass2_vars_new(ir_pass2 *self) {
  UNUSED(self);
  return ir_var_list_new();
}

int ir_pass2_vars_add(ir_pass2 *self, p_list vars, p_list vars1) {
  UNUSED(self);

  for (list_it it = vars1->head; it; it = it->next) {
    list_push_back(vars, it->data);
  }
  list_free(vars1);
  return 1;
}

ir_subroutine *ir_pass2_subroutine_new(ir_pass2 *self, ir_signature *signature,
                                       p_list vars, ir_block_base *cfg) {
  UNUSED(self);
  ir_subroutine *sub = ir_subroutine_new(self->file_name, signature, cfg, vars);
  return sub;
}

// TODO: validate vars and args
static int ir_pass1_subroutine_validate(ir_pass2 *self, ir_subroutine *sub) {
  UNUSED(self);
  UNUSED(sub);
  return 1;
}

int ir_pass2_subroutine_add(ir_pass2 *self, ir_subroutine *sub) {
  int     ok = 1;
  ir_ctx *ctx = self->ctx;

  if (!ir_pass1_subroutine_validate(self, sub)) {
    ok = 0;
  }

  hashset_it it;

  if (ok) {
    it = hashset_find(ctx->subroutines, sub);
    if (it.end(&it)) {
      error("subroutine is not found %s", ir_subroutine_name(sub));
      ok = 0;
    };
  }

  if (ok) {
    ir_subroutine *lsv = it.get(&it);

    ir_subroutine_status_enum lsv_status = ir_subroutine_status(lsv);
    ir_subroutine_status_enum sub_status = ir_subroutine_status(sub);

    if (lsv_status == IR_SUBROUTINE_STATUS_DEFINITION &&
        sub_status == IR_SUBROUTINE_STATUS_DEFINITION) {
      const int    subtype = EXCEPTION_IR_SUBROUTINE_REDEFINITION;
      const size_t line = sub->signature->id->base.loc.line;
      const size_t offset = sub->signature->id->base.loc.pos_start;
      const size_t prev_line = lsv->signature->id->base.loc.line;
      const size_t prev_offset = lsv->signature->id->base.loc.pos_start;
      exception   *ex = exception_new_f(
          EXCEPTION_IR, subtype, sub->file_name, line, offset,
          exception_subtype_ir_str(subtype), "previous definition in %s %d:%d",
          lsv->file_name, prev_line, prev_offset);
      ir_ctx_add_exception(ctx, ex);
      ok = 0;
    }
  }

  if (ok) {
    ir_scope *scope = ir_scope_stack_top(self->scope_stack);

    if (scope->type != IR_SCOPE_GLOBAL) {
      error("expected global scope to replace symbol %s",
            ir_subroutine_name(sub));
    } else {
      ir_scope_symbol_add_sub(scope, sub);
    }

    hashset_replace(ctx->subroutines, it, sub);
    debug("replaced subroutine %s", ir_subroutine_name(sub));

  } else {
    ir_subroutine_free(sub);
  }

  return ok;
}

// SCOPES
static void container_delete_scope(void *_data) { ir_scope_free(_data); }

p_list ir_pass2_scope_list_new(ir_pass2 *self) {
  UNUSED(self);
  return list_new(container_cmp_false, container_new_move,
                  container_delete_scope);
}

void ir_pass2_scope_add(ir_pass2 *self, p_list list, ir_scope *scope) {
  UNUSED(self);
  list_push_back(list, scope);
}

// BODY
void ir_pass2_body_before(ir_pass2 *self, ir_signature *sign, p_list vars) {
  self->break_stack =
      list_new(container_cmp_false, container_new_move, container_delete_false);

  ir_block_base *begin = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_block_base *end = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_block_base *cur = begin;

  ir_scope *scope = ir_scope_new(IR_SCOPE_SUBROUTINE, begin, end, cur);

  ir_scope_stack_push(self->scope_stack, scope);
  // debug("scope body pushed");

  for (list_it it = sign->params->head; it; it = it->next) {
    ir_var     *param = it->data;
    ir_term_id *k = ir_scope_symbol_k_var(param);

    if (ir_scope_stack_find_symbol(self->scope_stack, k)) {
      const int  subtype = EXCEPTION_IR_SYMBOL_REDEFINITION;
      exception *ex = exception_new_f(EXCEPTION_IR, subtype, self->file_name,
                                      k->base.loc.line, k->base.loc.pos_start,
                                      exception_subtype_ir_str(subtype),
                                      "symbol param %s is duplicated", k->name);
      ir_ctx_add_exception(self->ctx, ex);
    } else {
      ir_scope_symbol_add_var(scope, param);
    }
  }
  for (list_it it = vars->head; it; it = it->next) {
    ir_var     *var = it->data;
    ir_term_id *k = ir_scope_symbol_k_var(var);

    if (ir_scope_stack_find_symbol(self->scope_stack, k)) {
      const int  subtype = EXCEPTION_IR_SYMBOL_REDEFINITION;
      exception *ex = exception_new_f(EXCEPTION_IR, subtype, self->file_name,
                                      k->base.loc.line, k->base.loc.pos_start,
                                      exception_subtype_ir_str(subtype),
                                      "symbol arg %s is duplicated", k->name);
      ir_ctx_add_exception(self->ctx, ex);
    } else {
      ir_scope_symbol_add_var(scope, var);
    }
  }

  self->cfg = begin;

  list_push_front(self->break_stack, end);
  // debug("break_stack pushed");
}

ir_block_base *ir_pass2_body_build(ir_pass2 *self, ir_scope *child) {
  list_pop_front(self->break_stack);
  // debug("break_stack popped");
  list_free(self->break_stack);
  self->break_stack = NULL;
  // debug("break_stack freed");

  ir_scope *scope = ir_scope_stack_pop(self->scope_stack);

  scope->cur = child->end;
  ir_scope_free(child);

  // debug("%p -> %p", scope->cur, scope->end);
  ir_block_set_next(scope->cur, scope->end);
  scope->cur = scope->end;

  // debug("scope body popped");
  ir_scope_free(scope);
  // debug("scope body freed");

  ir_block_base *cfg = self->cfg;
  self->cfg = NULL;
  return cfg;
}

// BLOCK
void ir_pass2_block_before(ir_pass2 *self) {
  ir_scope *parent = ir_scope_stack_top(self->scope_stack);

  ir_block_base *end = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_scope *scope = ir_scope_new(IR_SCOPE_BLOCK, parent->cur, end, parent->cur);

  ir_scope_stack_push(self->scope_stack, scope);
  // debug("scope block pushed");

  // debug("break_stack pushed");
}

ir_scope *ir_pass2_block_build(ir_pass2 *self, p_list scopes) {
  // debug("break_stack popped");

  ir_scope *scope = ir_scope_stack_pop(self->scope_stack);

  // append child scopes
  for (list_it it = scopes->head; it; it = it->next) {
    ir_scope *child = it->data;
    // debug("linking_inside: %p -> %p", scope->cur, child->begin);
    ir_block_set_next(scope->cur, child->begin);
    scope->cur = child->end;
  }
  list_free(scopes);

  // update itself
  // debug("%p -> %p", scope->cur, scope->end);
  ir_block_set_next(scope->cur, scope->end);
  scope->cur = scope->end;

  scope->begin = ir_block_get_next(scope->begin);

  // debug("scope block popped");
  return scope;
}

// IF
void ir_pass2_if_before(ir_pass2 *self) {
  ir_scope *parent = ir_scope_stack_top(self->scope_stack);

  ir_block_base *end = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_scope *scope = ir_scope_new(IR_SCOPE_BLOCK, parent->cur, end, parent->cur);

  ir_scope_stack_push(self->scope_stack, scope);
}

ir_scope *ir_pass2_if_build(ir_pass2 *self, ir_expr_base *expr,
                            ir_scope *s_then, ir_scope *s_else) {
  ir_scope *scope = ir_scope_stack_pop(self->scope_stack);

  ir_block_base *cur = (ir_block_base *)ir_block_cond_new(
      s_else ? s_else->begin : scope->end, s_then->begin, expr);

  ir_block_set_next(s_then->end, scope->end);
  ir_scope_free(s_then);
  if (s_else) {
    ir_block_set_next(s_else->end, scope->end);
    ir_scope_free(s_else);
  }

  ir_block_set_next(scope->begin, cur);
  scope->begin = cur;

  return scope;
}

// WHILE
void ir_pass2_while_before(ir_pass2 *self) {
  ir_scope *parent = ir_scope_stack_top(self->scope_stack);

  ir_block_base *end = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_scope *scope = ir_scope_new(IR_SCOPE_BLOCK, parent->cur, end, parent->cur);

  ir_scope_stack_push(self->scope_stack, scope);
  list_push_front(self->break_stack, end);
}

ir_scope *ir_pass2_while_build(ir_pass2 *self, ir_expr_base *expr,
                               ir_scope *s_child) {
  list_pop_front(self->break_stack);

  ir_scope *scope = ir_scope_stack_pop(self->scope_stack);

  ir_block_base *cur =
      (ir_block_base *)ir_block_cond_new(scope->end, s_child->begin, expr);

  ir_block_set_next(s_child->end, cur);
  ir_scope_free(s_child);

  ir_block_set_next(scope->begin, cur);
  scope->begin = cur;

  return scope;
}

// DO
void ir_pass2_do_before(ir_pass2 *self) {
  ir_scope *parent = ir_scope_stack_top(self->scope_stack);

  ir_block_base *end = (ir_block_base *)ir_block_next_new(NULL, NULL);
  ir_scope *scope = ir_scope_new(IR_SCOPE_BLOCK, parent->cur, end, parent->cur);

  ir_scope_stack_push(self->scope_stack, scope);
  list_push_front(self->break_stack, end);
}

ir_scope *ir_pass2_do_build(ir_pass2 *self, ir_expr_base *expr, int p,
                            ir_scope *s_child) {
  list_pop_front(self->break_stack);

  ir_scope *scope = ir_scope_stack_pop(self->scope_stack);

  ir_block_base *cur;
  if (p) {
    cur = (ir_block_base *)ir_block_cond_new(scope->end, s_child->begin, expr);
  } else {
    cur = (ir_block_base *)ir_block_cond_new(s_child->begin, scope->end, expr);
  }

  ir_block_set_next(s_child->end, cur);

  ir_block_set_next(scope->begin, s_child->begin);
  scope->begin = s_child->begin;

  ir_scope_free(s_child);

  return scope;
}

// BREAK
static ir_expr_base *ir_pass2_expr_true() {
  ir_term_lit_v v;
  v.v_bool = 1;
  return (ir_expr_base *)ir_expr_lit_new(
      ir_term_lit_new((ir_loc){}, IR_T_BOOL, v));
}

void ir_pass2_break_before(ir_pass2 *self) { UNUSED(self); }

ir_scope *ir_pass2_break_build(ir_pass2 *self) {
  ir_block_cond *cur = ir_block_cond_new(NULL, list_front(self->break_stack),
                                         ir_pass2_expr_true());
  ir_scope      *scope = ir_scope_new(IR_SCOPE_INLINE, (ir_block_base *)cur,
                                      (ir_block_base *)cur, (ir_block_base *)cur);
  // debug("scope break %p", scope);
  return scope;
}

void ir_pass2_expression_before(ir_pass2 *self) { UNUSED(self); }

ir_scope *ir_pass2_expression_build(ir_pass2 *self, ir_expr_base *expr) {
  UNUSED(self);
  ir_block_base *cur = (ir_block_base *)ir_block_next_new(NULL, expr);
  ir_scope      *scope = ir_scope_new(IR_SCOPE_INLINE, (ir_block_base *)cur,
                                      (ir_block_base *)cur, (ir_block_base *)cur);
  // debug("scope expression %p", scope);
  return scope;
}
