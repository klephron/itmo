// #include "ir/scope.h"
// #include "util/container_util.h"
// #include "util/log.h"
// #include "util/macro.h"

// #include <stdlib.h>

// static int    container_cmp_scope_symbol_kv(const void *_lsv, const void
// *_rsv); static size_t container_hash_scope_symbol_kv(const void *_lsv);
// static void   container_delete_scope_symbol_kv(void *_lsv);

// // SCOPE
// static const char *scope_ttstr(ir_scope_enum type) {
//   switch (type) {
//   case IR_SCOPE_GLOBAL:
//     return "GLOBAL";
//   case IR_SCOPE_SUBROUTINE:
//     return "SUBROUTINE";
//   case IR_SCOPE_BLOCK:
//     return "BLOCK";
//   case IR_SCOPE_INLINE:
//     return "INLINE";
//   default:
//     return "UNKNOWN";
//   }
// }

// void ir_scope_free(ir_scope *self) {
//   if (self) {
//     hashset_free(self->symbols);
//     free(self);
//   }
// }

// ir_scope *ir_scope_new(ir_scope_enum type, ir_block_base *begin,
//                        ir_block_base *end, ir_block_base *cur) {
//   ir_scope *self = MALLOC(ir_scope);
//   self->type = type;
//   self->begin = begin;
//   self->end = end;
//   self->cur = cur;

//   self->symbols = hashset_new(container_cmp_scope_symbol_kv,
//   container_new_move,
//                               container_delete_scope_symbol_kv,
//                               container_hash_scope_symbol_kv);
//   return self;
// }

// // SYMBOLS
// static int container_cmp_scope_symbol_kv(const void *_lsv, const void *_rsv)
// {
//   const ir_scope_symbol_kv *lsv = _lsv;
//   const ir_scope_symbol_kv *rsv = _rsv;
//   return !ir_term_id_equals(lsv->k, rsv->k);
// }

// static size_t container_hash_scope_symbol_kv(const void *_lsv) {
//   const ir_scope_symbol_kv *lsv = _lsv;
//   return container_hash_chars(lsv->k->name);
// }

// static void container_delete_scope_symbol_kv(void *_lsv) {
//   ir_scope_symbol_kv_free(_lsv);
// }

// static void ir_scope_symbol_base_init(ir_scope_symbol_base *base,
//                                       ir_scope_symbol_enum  kind) {
//   base->kind = kind;
// }

// void ir_scope_symbol_free(ir_scope_symbol_base *generic) {
//   if (generic) {
//     free(generic);
//   }
// }

// ir_scope_symbol_sub *ir_scope_symbol_sub_new(ir_subroutine *sub) {
//   ir_scope_symbol_sub *self = MALLOC(ir_scope_symbol_sub);
//   ir_scope_symbol_base_init(&self->base, IR_SCOPE_SYMBOL_SUBROUTINE);
//   self->value = sub;
//   return self;
// }

// ir_scope_symbol_var *ir_scope_symbol_var_new(ir_var *var) {
//   ir_scope_symbol_var *self = MALLOC(ir_scope_symbol_var);
//   ir_scope_symbol_base_init(&self->base, IR_SCOPE_SYMBOL_VARIABLE);
//   self->value = var;
//   return self;
// }

// int ir_scope_symbol_cmp(const ir_scope_symbol_base *lsv,
//                         const ir_scope_symbol_base *rsv) {
//   if (lsv->kind != rsv->kind) {
//     return 1;
//   }
//   switch (lsv->kind) {
//   case IR_SCOPE_SYMBOL_SUBROUTINE: {
//     ir_scope_symbol_sub *lsvs = (ir_scope_symbol_sub *)lsv;
//     ir_scope_symbol_sub *rsvs = (ir_scope_symbol_sub *)rsv;
//     return ir_subroutine_cmp(lsvs->value, rsvs->value);
//   }
//   case IR_SCOPE_SYMBOL_VARIABLE: {
//     ir_scope_symbol_var *lsvs = (ir_scope_symbol_var *)lsv;
//     ir_scope_symbol_var *rsvs = (ir_scope_symbol_var *)rsv;
//     return ir_var_cmp(lsvs->value, rsvs->value);
//   }
//   default:
//     error("unknown scope symbol kind");
//     return 1;
//   }
// }

// ir_scope_symbol_kv *ir_scope_symbol_kv_new(ir_term_id           *k,
//                                            ir_scope_symbol_base *v) {
//   ir_scope_symbol_kv *kv = MALLOC(ir_scope_symbol_kv);
//   kv->k = k;
//   kv->v = v;
//   return kv;
// }

// void ir_scope_symbol_kv_free(ir_scope_symbol_kv *kv) {
//   if (kv) {
//     ir_scope_symbol_free(kv->v);
//     free(kv);
//   }
// }

// ir_term_id *ir_scope_symbol_k_sub(ir_subroutine *sub) {
//   return sub->signature->id;
// }
// ir_term_id *ir_scope_symbol_k_var(ir_var *sub) { return sub->id; }

// void ir_scope_symbol_add(ir_scope *scope, ir_scope_symbol_kv *kv) {
//   hashset_insert(scope->symbols, kv);
//   debug("added symbol %s to scope %s", kv->k->name,
//   scope_ttstr(scope->type));
// }

// void ir_scope_symbol_add_sub(ir_scope *scope, ir_subroutine *sub) {
//   ir_scope_symbol_kv *kv = ir_scope_symbol_kv_new(
//       ir_scope_symbol_k_sub(sub),
//       (ir_scope_symbol_base *)ir_scope_symbol_sub_new(sub));
//   ir_scope_symbol_add(scope, kv);
// }

// void ir_scope_symbol_add_var(ir_scope *scope, ir_var *var) {
//   ir_scope_symbol_kv *kv = ir_scope_symbol_kv_new(
//       ir_scope_symbol_k_var(var),
//       (ir_scope_symbol_base *)ir_scope_symbol_var_new(var));
//   ir_scope_symbol_add(scope, kv);
// }

// ir_scope_symbol_base *ir_scope_symbol_find(ir_scope         *scope,
//                                            const ir_term_id *id) {
//   hashset_it it = hashset_find(scope->symbols, &(ir_scope_symbol_kv){.k =
//   id}); if (!it.end(&it)) {
//     return ((ir_scope_symbol_kv *)it.get(&it))->v;
//   }
//   return NULL;
// }

// // SCOPE STACK
// static void container_delete_scope(void *data) {
//   ir_scope *scope = data;
//   ir_scope_free(scope);
// }

// scope_stack ir_scope_stack_new() {
//   return (scope_stack){
//       .list = list_new(container_cmp_false, container_new_move,
//                        container_delete_scope),
//   };
// }

// void ir_scope_stack_free(scope_stack stack) { return list_free(stack.list); }

// void ir_scope_stack_push(scope_stack stack, ir_scope *scope) {
//   list_push_front(stack.list, scope);
// }

// ir_scope *ir_scope_stack_pop(scope_stack stack) {
//   ir_scope *scope = list_pop_front(stack.list);
//   return scope;
// }

// ir_scope *ir_scope_stack_top(scope_stack stack) {
//   return list_front(stack.list);
// }

// ir_scope_symbol_base *ir_scope_stack_find_symbol(scope_stack       stack,
//                                                  const ir_term_id *id) {
//   ir_scope_symbol_base *sym = NULL;
//   for (list_it it = stack.list->head; it; it = it->next) {
//     sym = ir_scope_symbol_find(it->data, id);
//     if (sym) {
//       break;
//     }
//   }
//   return sym;
// }
