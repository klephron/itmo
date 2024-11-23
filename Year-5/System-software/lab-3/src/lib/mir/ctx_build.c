#include "ctx_build.h"

#include "util/log.h"
#include <string.h>

// wraps result in store expression
static mir_node_base *mir_ctx_build_expr_store(mir_ctx        *ctx,
                                               mir_subroutine *mir_sub,
                                               mir_bb         *mir_bb,
                                               mir_node_base  *mir_expr) {
  mir_value *store_value = mir_value_new(ctx->value_cnt++, NULL);
  list_mir_value_push_back(mir_sub->values, store_value);

  list_mir_arg *store_args = list_mir_arg_new();
  list_mir_arg_push_back(store_args, (mir_node_base *)store_value);
  list_mir_arg_push_back(store_args, (mir_node_base *)mir_expr);

  list_mir_node_push_back(
      mir_bb->stmts,
      (mir_node_base *)mir_builtin_new(MIR_BUILTIN_STORE, store_args, NULL));

  return (mir_node_base *)store_value;
}

// if ret_value  -> returns mir_value (intermediate)
// if !ret_value -> returns expression
static mir_node_base *mir_ctx_build_expr(mir_ctx *ctx, mir_subroutine *mir_sub,
                                         mir_bb              *mir_bb,
                                         const eir_expr_base *eir_expr,
                                         int                  ret_value) {
  if (!eir_expr) {
    return NULL;
  }

  switch (eir_expr->kind) {
  case EIR_EXPR_UNARY: {
    const eir_expr_unary *unary = (eir_expr_unary *)eir_expr;

    mir_node_base *first =
        mir_ctx_build_expr(ctx, mir_sub, mir_bb, unary->first, 1);

    if (unary->op == EIR_EXPR_UNARY_LOAD) {
      return first;
    }

    mir_builtin_enum kind;
    switch (unary->op) {
    case EIR_EXPR_UNARY_PLUS:
      kind = MIR_BUILTIN_PLUS;
      break;
    case EIR_EXPR_UNARY_MINUS:
      kind = MIR_BUILTIN_MINUS;
      break;
    case EIR_EXPR_UNARY_LOGICAL_NOT:
      kind = MIR_BUILTIN_LOGICAL_NOT;
      break;
    case EIR_EXPR_UNARY_BITWISE_NOT:
      kind = MIR_BUILTIN_BITWISE_NOT;
      break;
    case EIR_EXPR_UNARY_INC:
      kind = MIR_BUILTIN_INC;
      break;
    case EIR_EXPR_UNARY_DEC:
      kind = MIR_BUILTIN_DEC;
      break;
    default:
      error("unexpected unary op %d", unary->op);
      kind = MIR_BUILTIN_UNKNOWN;
      break;
    }

    list_mir_arg *args = list_mir_arg_new();
    list_mir_arg_push_back(args, (mir_node_base *)first);

    mir_builtin *expr = mir_builtin_new(kind, args, NULL);

    if (ret_value) {
      return mir_ctx_build_expr_store(ctx, mir_sub, mir_bb,
                                      (mir_node_base *)expr);
    } else {
      return (mir_node_base *)expr;
    }

  } break;
  case EIR_EXPR_BINARY: {
    const eir_expr_binary *binary = (eir_expr_binary *)eir_expr;

    // HANDLE BINARY_STORE
    if (binary->op == EIR_EXPR_BINARY_STORE) {
      mir_node_base *store_second =
          mir_ctx_build_expr(ctx, mir_sub, mir_bb, binary->second, 1);

      const eir_expr_base *first = binary->first;
      if (!(first->kind == EIR_EXPR_IDENTIFIER ||
            first->kind == EIR_EXPR_INDEX)) {
        error("expected identifier or index, got %d %p", first->kind, first);
        mir_node_free(store_second);
        return NULL;
      }

      list_mir_arg *store_args = list_mir_arg_new();

      mir_node_base *store_first;
      if (first->kind == EIR_EXPR_IDENTIFIER) {
        hashset_mir_map_value_it map_id = hashset_mir_map_value_find(
            ctx->map_lvalues,
            &(mir_map_value){.hir = ((eir_expr_id *)first)->binding});

        if (map_id.end(&map_id)) {
          error("unknown identifier %p", first);
          mir_node_free(store_second);
          return NULL;
        }

        store_first = (mir_node_base *)map_id.get(&map_id)->mir;
      } else {
        store_first =
            mir_ctx_build_expr(ctx, mir_sub, mir_bb, binary->first, 0);
      }

      list_mir_arg_push_back(store_args, store_first);
      list_mir_arg_push_back(store_args, store_second);

      mir_node_base *store =
          (mir_node_base *)mir_builtin_new(MIR_BUILTIN_STORE, store_args, NULL);

      if (ret_value) {
        list_mir_node_push_back(mir_bb->stmts, store);

        if (first->kind == EIR_EXPR_IDENTIFIER) {
          return store_first;
        } else {
          return mir_ctx_build_expr_store(
              ctx, mir_sub, mir_bb,
              mir_ctx_build_expr(ctx, mir_sub, mir_bb, binary->first, 0));
        }
      } else {
        return store;
      }
    }

    // HANDLE OTHER
    mir_builtin_enum kind;
    switch (binary->op) {
    case EIR_EXPR_BINARY_LOGICAL_OR:
      kind = MIR_BUILTIN_LOGICAL_OR;
      break;
    case EIR_EXPR_BINARY_LOGICAL_AND:
      kind = MIR_BUILTIN_LOGICAL_AND;
      break;
    case EIR_EXPR_BINARY_BITWISE_OR:
      kind = MIR_BUILTIN_BITWISE_OR;
      break;
    case EIR_EXPR_BINARY_BITWISE_XOR:
      kind = MIR_BUILTIN_BITWISE_XOR;
      break;
    case EIR_EXPR_BINARY_BITWISE_AND:
      kind = MIR_BUILTIN_BITWISE_AND;
      break;
    case EIR_EXPR_BINARY_EQUALS:
      kind = MIR_BUILTIN_EQUALS;
      break;
    case EIR_EXPR_BINARY_NOT_EQUALS:
      kind = MIR_BUILTIN_NOT_EQUALS;
      break;
    case EIR_EXPR_BINARY_LESS:
      kind = MIR_BUILTIN_LESS;
      break;
    case EIR_EXPR_BINARY_LESS_EQUALS:
      kind = MIR_BUILTIN_LESS_EQUALS;
      break;
    case EIR_EXPR_BINARY_GREATER:
      kind = MIR_BUILTIN_GREATER;
      break;
    case EIR_EXPR_BINARY_GREATER_EQUALS:
      kind = MIR_BUILTIN_GREATER_EQUALS;
      break;
    case EIR_EXPR_BINARY_BITWISE_SHIFT_LEFT:
      kind = MIR_BUILTIN_BITWISE_SHIFT_LEFT;
      break;
    case EIR_EXPR_BINARY_BITWISE_SHIFT_RIGHT:
      kind = MIR_BUILTIN_BITWISE_SHIFT_RIGHT;
      break;
    case EIR_EXPR_BINARY_ADD:
      kind = MIR_BUILTIN_ADD;
      break;
    case EIR_EXPR_BINARY_SUB:
      kind = MIR_BUILTIN_SUB;
      break;
    case EIR_EXPR_BINARY_MUL:
      kind = MIR_BUILTIN_MUL;
      break;
    case EIR_EXPR_BINARY_DIV:
      kind = MIR_BUILTIN_DIV;
      break;
    case EIR_EXPR_BINARY_MOD:
      kind = MIR_BUILTIN_MOD;
      break;
    default:
      kind = MIR_BUILTIN_UNKNOWN;
      error("unexpected eir type %d %p", binary->op, binary);
      break;
    }

    if (kind == MIR_BUILTIN_UNKNOWN) {
      return NULL;
    }

    list_mir_arg *args = list_mir_arg_new();
    mir_value    *args_first =
        (mir_value *)mir_ctx_build_expr(ctx, mir_sub, mir_bb, binary->first, 1);
    list_mir_arg_push_back(args, (mir_node_base *)args_first);
    mir_value *args_second = (mir_value *)mir_ctx_build_expr(
        ctx, mir_sub, mir_bb, binary->second, 1);
    list_mir_arg_push_back(args, (mir_node_base *)args_second);

    mir_builtin *expr = mir_builtin_new(kind, args, NULL);

    if (ret_value) {
      return mir_ctx_build_expr_store(ctx, mir_sub, mir_bb,
                                      (mir_node_base *)expr);
    } else {
      return (mir_node_base *)expr;
    }

  } break;
  case EIR_EXPR_LITERAL: {
    const eir_expr_lit *lit = (eir_expr_lit *)eir_expr;
    mir_value *val = mir_value_new(ctx->value_cnt++, type_copy(lit->lit->type));
    list_mir_value_push_back(mir_sub->values, val);

    list_mir_arg *args = list_mir_arg_new();
    list_mir_arg_push_back(args, (mir_node_base *)val);
    list_mir_arg_push_back(
        args, (mir_node_base *)mir_lit_new(hir_lit_copy(lit->lit)));

    list_mir_node_push_back(mir_bb->stmts, (mir_node_base *)mir_builtin_new(
                                               MIR_BUILTIN_STORE, args, NULL));
    return (mir_node_base *)val;
  } break;
  case EIR_EXPR_IDENTIFIER: {
    const eir_expr_id *id = (eir_expr_id *)eir_expr;

    // identifier
    hashset_mir_map_value_it v_it = hashset_mir_map_value_find(
        ctx->map_lvalues, &(mir_map_value){.hir = id->binding});
    if (!v_it.end(&v_it)) {
      const mir_value *val = v_it.get(&v_it)->mir;
      return (mir_node_base *)val;
    }

    hashset_mir_map_sub_it s_it = hashset_mir_map_sub_find(
        ctx->map_subs, &(mir_map_sub){.hir = id->binding});
    if (!s_it.end(&s_it)) {
      const mir_node_base *val = s_it.get(&s_it)->mir;
      return (mir_node_base *)val;
    }

    error("can't find id from local index matching %s", id->binding->name);
    return NULL;

  } break;
  case EIR_EXPR_CALL: {
    const eir_expr_call *call = (eir_expr_call *)eir_expr;

    list_mir_arg *args = list_mir_arg_new();

    mir_node_base *callee =
        mir_ctx_build_expr(ctx, mir_sub, mir_bb, call->callee, 1);
    list_mir_arg_push_back(args, callee);

    for (list_eir_expr_it it = list_eir_expr_begin(call->args); !it.end(&it);
         it.next(&it)) {
      const eir_expr_base *eir_arg = it.get(&it);
      mir_node_base       *mir_arg =
          mir_ctx_build_expr(ctx, mir_sub, mir_bb, eir_arg, 1);
      list_mir_arg_push_back(args, mir_arg);
    }

    mir_builtin *expr = mir_builtin_new(MIR_BUILTIN_CALL, args, NULL);

    if (ret_value) {
      return mir_ctx_build_expr_store(ctx, mir_sub, mir_bb,
                                      (mir_node_base *)expr);
    } else {
      return (mir_node_base *)expr;
    }
  } break;
  case EIR_EXPR_INDEX: {
    const eir_expr_index *index = (eir_expr_index *)eir_expr;

    list_mir_arg *args = list_mir_arg_new();

    mir_node_base *indexed =
        mir_ctx_build_expr(ctx, mir_sub, mir_bb, index->indexed, 1);
    list_mir_arg_push_back(args, indexed);

    for (list_eir_expr_it it = list_eir_expr_begin(index->args); !it.end(&it);
         it.next(&it)) {
      const eir_expr_base *eir_arg = it.get(&it);
      mir_node_base       *mir_arg =
          mir_ctx_build_expr(ctx, mir_sub, mir_bb, eir_arg, 1);
      list_mir_arg_push_back(args, mir_arg);
    }

    mir_builtin *expr = mir_builtin_new(MIR_BUILTIN_INDEX, args, NULL);

    if (ret_value) {
      return mir_ctx_build_expr_store(ctx, mir_sub, mir_bb,
                                      (mir_node_base *)expr);
    } else {
      return (mir_node_base *)expr;
    }

  } break;
  case EIR_EXPR_BUILTIN: {
    const eir_expr_builtin *builtin = (eir_expr_builtin *)eir_expr;

    type_base *spec = type_copy(builtin->base.type);

    mir_builtin_enum kind;
    switch (builtin->kind) {
    case EIR_EXPR_BUILTIN_CAST:
      kind = MIR_BUILTIN_CAST;
      break;
    case EIR_EXPR_BUILTIN_NEW:
      kind = MIR_BUILTIN_NEW;
      break;
    case EIR_EXPR_BUILTIN_FREE:
      kind = MIR_BUILTIN_FREE;
      break;
    default:
      error("unknown builtin kind %d %p", builtin->kind, builtin);
      return NULL;
    }

    list_mir_arg *args = list_mir_arg_new();
    for (list_eir_expr_it it = list_eir_expr_begin(builtin->args); !it.end(&it);
         it.next(&it)) {
      const eir_expr_base *eir_arg = it.get(&it);
      mir_node_base       *mir_arg =
          mir_ctx_build_expr(ctx, mir_sub, mir_bb, eir_arg, 1);
      list_mir_arg_push_back(args, mir_arg);
    }

    mir_builtin *expr = mir_builtin_new(kind, args, spec);
    if (ret_value) {
      return mir_ctx_build_expr_store(ctx, mir_sub, mir_bb,
                                      (mir_node_base *)expr);
    } else {
      return (mir_node_base *)expr;
    }
  } break;
  default:
    error("unexpected eir expr type %d %p", eir_expr->kind, eir_expr);
    break;
  }

  return NULL;
}

static mir_bb *mir_ctx_build_stmt(mir_ctx *ctx, mir_subroutine *mir_sub,
                                  const eir_block_base *eir_bb) {
  if (!eir_bb) {
    return NULL;
  }

  // check if mapping exists
  hashset_mir_map_bb_it it =
      hashset_mir_map_bb_find(ctx->map_bbs, &(mir_map_bb){.hir = eir_bb});
  if (!it.end(&it)) {
    return (mir_bb *)it.get(&it)->mir;
  }

  mir_bb *bb = mir_bb_new(eir_bb->id, list_mir_node_new(), NULL);

  hashset_mir_map_bb_insert(ctx->map_bbs, mir_map_bb_new(eir_bb, bb));
  list_mir_bb_push_back(mir_sub->bbs, bb);

  switch (eir_bb->kind) {
  case EIR_BLOCK_NEXT: {
    eir_block_next *eir_next = (eir_block_next *)eir_bb;

    mir_node_base *expr =
        mir_ctx_build_expr(ctx, mir_sub, bb, eir_next->expr, 0);
    if (expr) {
      list_mir_node_push_back(bb->stmts, expr);
    }

    mir_bb *bb_next = mir_ctx_build_stmt(ctx, mir_sub, eir_next->next);

    if (bb_next) {
      bb->jmp = mir_jmp_new(NULL, NULL, bb_next);
    }
  } break;
  case EIR_BLOCK_COND: {
    eir_block_cond *eir_cond = (eir_block_cond *)eir_bb;

    mir_value *value =
        (mir_value *)mir_ctx_build_expr(ctx, mir_sub, bb, eir_cond->cond, 1);
    mir_bb *bb_je = mir_ctx_build_stmt(ctx, mir_sub, eir_cond->je);
    mir_bb *bb_jz = mir_ctx_build_stmt(ctx, mir_sub, eir_cond->jz);

    if (bb_jz || bb_je) {
      bb->jmp = mir_jmp_new(value, bb_je, bb_jz);
    }
  } break;
  default:
    error("unexpected block kind %d %p", eir_bb->kind, eir_bb);
    bb = NULL;
    break;
  }
  return bb;
}

static void mir_ctx_build_subroutine_before(mir_ctx              *ctx,
                                            const eir_subroutine *eir_sub) {
  const type_callable *eir_type = (type_callable *)eir_sub->binding->type;
  type_enum            eir_type_enum = eir_type->base.type;

  if (eir_type_enum != TYPE_CALLABLE) {
    error("expected callable, got %s %p", type_enum_str(eir_type_enum),
          eir_sub);
  }

  char *mir_name = strdup(eir_sub->binding->name);

  if (!eir_sub->root) {
    type_base *mir_ret = type_copy(eir_type->ret_type);
    if (!mir_ret) {
      mir_ret = type_base_new(NULL, TYPE_VOID);
    }
    list_type *mir_param_types = list_type_copy(eir_type->param_types);

    mir_subroutine_ext *mir_sub =
        mir_subroutine_ext_new(mir_name, mir_ret, mir_param_types);
    list_mir_subroutine_ext_push_back(ctx->mir->sub_exts, mir_sub);

    // add to mapping
    hashset_mir_map_sub_insert(
        ctx->map_subs,
        mir_map_sub_new(eir_sub->binding, (mir_node_base *)mir_sub));
  } else {
    mir_subroutine *mir_sub =
        mir_subroutine_new(mir_name, NULL, list_mir_value_new(),
                           list_mir_value_new(), list_mir_bb_new());

    list_mir_subroutine_push_back(ctx->mir->subs, mir_sub);

    // add to mapping
    hashset_mir_map_sub_insert(
        ctx->map_subs,
        mir_map_sub_new(eir_sub->binding, (mir_node_base *)mir_sub));
  }
}

static void mir_ctx_build_subroutine(mir_ctx              *ctx,
                                     const eir_subroutine *eir_sub) {
  if (!eir_sub->root) {
    return;
  }

  // get mapping
  hashset_mir_map_sub_it sub_it = hashset_mir_map_sub_find(
      ctx->map_subs, &(mir_map_sub){.hir = eir_sub->binding});
  if (sub_it.end(&sub_it)) {
    error("can't get preserved subroutine from index for %p", eir_sub);
    return;
  }
  mir_subroutine *mir_sub = (mir_subroutine *)sub_it.get(&sub_it)->mir;

  ctx->map_lvalues = hashset_mir_map_value_new();
  ctx->value_cnt = 0;

  const type_callable *eir_type = (type_callable *)eir_sub->binding->type;

  type_base *mir_ret_type = type_copy(eir_type->ret_type);
  if (!mir_ret_type) {
    mir_ret_type = type_base_new(NULL, TYPE_VOID);
  }
  mir_value *mir_ret = mir_value_new(ctx->value_cnt++, mir_ret_type);
  mir_sub->ret = mir_ret;

  for (list_hir_param_it it = list_hir_param_begin(eir_sub->params_ref);
       !it.end(&it); it.next(&it)) {
    hir_param *param = it.get(&it);
    mir_value *value = mir_value_new(ctx->value_cnt++, type_copy(param->type));
    list_mir_value_push_back(mir_sub->args, value);
    // add to local var index
    hashset_mir_map_value_insert(ctx->map_lvalues,
                                 mir_map_value_new(param->binding, value));
  }

  for (list_hir_var_it it = list_hir_var_begin(eir_sub->vars_ref); !it.end(&it);
       it.next(&it)) {
    hir_var   *var = it.get(&it);
    mir_value *value = mir_value_new(ctx->value_cnt++, type_copy(var->type));
    list_mir_value_push_back(mir_sub->values, value);
    // add to local var index
    hashset_mir_map_value_insert(ctx->map_lvalues,
                                 mir_map_value_new(var->binding, value));
  }

  ctx->map_bbs = hashset_mir_map_bb_new();
  mir_ctx_build_stmt(ctx, mir_sub, eir_sub->root);
  hashset_mir_map_bb_free(ctx->map_bbs);

  hashset_mir_map_value_free(ctx->map_lvalues);
}

void mir_ctx_build_all(mir_ctx *ctx, const eir *eir) {
  for (list_eir_subroutine_it it = list_eir_subroutine_begin(eir->subroutines);
       !it.end(&it); it.next(&it)) {
    const eir_subroutine *sub = it.get(&it);
    mir_ctx_build_subroutine_before(ctx, sub);
  }
  for (list_eir_subroutine_it it = list_eir_subroutine_begin(eir->subroutines);
       !it.end(&it); it.next(&it)) {
    const eir_subroutine *sub = it.get(&it);
    mir_ctx_build_subroutine(ctx, sub);
  }
}
