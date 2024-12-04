#include "util/log.h"
#include "util/macro.h"
#include "x86_64_ctx.h"

#include "x86_64_core/x86_64_core.h"
#include "x86_64_core/x86_64_meta.h"

#include <stdio.h>
#include <string.h>

typedef enum cg_reg_type_enum {
  CG_REG_UNKNOWN,
  CG_REG_CALLER,
  CG_REG_CALLEE,
} cg_reg_type;

static int cg_is_exit(cg_x86_64_ctx *self, const mir_subroutine *sub) {
  UNUSED(self);
  return !strcmp(sub->name, "main");
}

static int cg_is_global(cg_x86_64_ctx *self, const mir_node_base *base) {
  hashset_cg_x86_64_mir_ref_it it =
      hashset_cg_x86_64_mir_ref_find(self->globls, (mir_node_base *)base);
  return !it.end(&it);
}

static char *cg_local_name(const char *old) {
  size_t len = strlen(old);
  char  *name = malloc(len + 3);
  memcpy(name, ".L", 2);
  memcpy(name + 2, old, len + 1);
  return name;
}

static void cg_sub_prologue(cg_x86_64_ctx *self) {
  list_cg_x86_64_mode *operands;

  // pushq %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_unit_push_back(
      self->sub_units_ref,
      (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_PUSHQ, operands));
  // movq %rsp, %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RSP));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_unit_push_back(
      self->sub_units_ref,
      (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

  self->sub_stack_size += X86_64_ALIGN_REG_64;
  self->sub_rbp_offset = self->sub_stack_size;
}

static char *cg_sub_epilogue_name(mir_subroutine *sub) {
  const char epilogue[] = "_ep";

  size_t ssz = strlen(sub->name);
  size_t lsz = ssz + sizeof(epilogue);
  char  *label = malloc(lsz);
  memcpy(label, sub->name, ssz);
  memcpy(label + ssz, epilogue, sizeof(epilogue));
  char *local = cg_local_name(label);
  free(label);
  return local;
}

static void cg_sub_epilogue(cg_x86_64_ctx *self, mir_subroutine *sub, int ret) {
  cg_x86_64_label *block_label =
      cg_x86_64_label_new(cg_sub_epilogue_name(sub), CG_X86_64_LABEL_INSTR);
  list_cg_x86_64_unit_push_back(self->sub_units_ref,
                                (cg_x86_64_unit *)block_label);

  list_cg_x86_64_mode *operands;
  // movq %rsp, %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RSP));
  list_cg_x86_64_unit_push_back(
      self->sub_units_ref,
      (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));
  // popq %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_unit_push_back(
      self->sub_units_ref,
      (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_POPQ, operands));

  self->sub_stack_size -= X86_64_ALIGN_REG_64;
  self->sub_rbp_offset = 0;

  // ret
  if (ret) {
    operands = list_cg_x86_64_mode_new();
    list_cg_x86_64_unit_push_back(
        self->sub_units_ref,
        (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_RETQ, operands));
    self->sub_stack_size -= X86_64_ALIGN_REG_64;
  }
}

static void cg_sub_exit(cg_x86_64_ctx *self) {
  list_cg_x86_64_mode *operands;

  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands, cg_x86_64_mode_new_immediate(60));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RAX));
  list_cg_x86_64_unit_push_back(
      self->sub_units_ref,
      (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_MOVQ, operands));

  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RDI));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RDI));
  list_cg_x86_64_unit_push_back(
      self->sub_units_ref,
      (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_XORQ, operands));

  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_unit_push_back(
      self->sub_units_ref,
      (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_SYSCALL, operands));
}

static size_t cg_with_align(size_t size, size_t align) {
  return ((size - 1) / align + 1) * align;
}

// static void cg_add_value_init(cg_x86_64_ctx *self, mir_value *value,
//                               size_t rsp_off) {
//   list_cg_x86_64_mode *operands;

//   operands = list_cg_x86_64_mode_new();
//   list_cg_x86_64_mode_push_back(
//       operands, cg_x86_64_mode_new_base(rsp_off, CG_X86_64_REG_RBP));
//   list_cg_x86_64_mode_push_back(operands,
//                                 cg_x86_64_mode_new_register(CG_X86_64_REG_RDI));
//   list_cg_x86_64_unit_push_back(
//       self->sub_units_ref,
//       (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_LEAQ, operands));

//   operands = list_cg_x86_64_mode_new();
//   list_cg_x86_64_mode_push_back(
//       operands, cg_x86_64_mode_new_direct(
//                     strdup(x86_64_symbol_enum_str(X86_64_SYMBOL_VALUE_INIT))));
//   list_cg_x86_64_unit_push_back(
//       self->sub_units_ref,
//       (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_CALL, operands));
// }

static void cg_value_prologue(cg_x86_64_ctx *self, mir_subroutine *sub) {
  list_cg_x86_64_mode *operands;

  { // reserve memory on stack
    size_t values = 0;

    values += sub->ret ? 1 : 0;
    values += list_mir_value_size(sub->args);
    values += list_mir_value_size(sub->vars);
    values += list_mir_value_size(sub->tmps);

    size_t rsp_sub =
        cg_with_align(values * sizeof(x86_64_value), X86_64_ALIGN_CALL);

    operands = list_cg_x86_64_mode_new();
    list_cg_x86_64_mode_push_back(operands,
                                  cg_x86_64_mode_new_immediate(rsp_sub));
    list_cg_x86_64_mode_push_back(
        operands, cg_x86_64_mode_new_register(CG_X86_64_REG_RSP));
    list_cg_x86_64_unit_push_back(
        self->sub_units_ref,
        (cg_x86_64_unit *)cg_x86_64_instr_new(CG_X86_64_MNEM_SUBQ, operands));

    self->sub_stack_size += rsp_sub;
  }

  // index values based on rbp
  size_t rbp_offset = 0;
  if (sub->ret) {
    hashset_cg_x86_64_map_value_insert(
        self->sub_map_values, cg_x86_64_map_value_new(sub->ret, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->args); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        self->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->vars); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        self->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->tmps); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        self->sub_map_values, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }
}

static char *cg_block_name(mir_subroutine *sub, mir_bb *bb) {
  size_t bb_sz = strlen(sub->name) + 1 + 32;
  char  *bb_name = malloc(bb_sz);
  snprintf(bb_name, bb_sz, "%s_%zu", sub->name, bb->id);
  char *local = cg_local_name(bb_name);
  free(bb_name);
  return local;
}

static void cg_stmt_store(cg_x86_64_ctx *self, mir_subroutine *sub, mir_bb *bb,
                          mir_builtin *stmt) {
  UNUSED(self);
  UNUSED(sub);
  UNUSED(bb);
  UNUSED(stmt);
}

static void cg_stmt_drop(cg_x86_64_ctx *self, mir_subroutine *sub, mir_bb *bb,
                         mir_builtin *stmt) {
  UNUSED(self);
  UNUSED(sub);
  UNUSED(bb);
  UNUSED(stmt);
}

// no jump only on last block
static void cg_jump(cg_x86_64_ctx *self, mir_subroutine *sub, mir_bb *bb,
                    mir_jmp *jmp) {
  UNUSED(self);
  UNUSED(sub);
  UNUSED(bb);
  if (!jmp) {
    return;
  }
}

static void cg_stmt(cg_x86_64_ctx *self, mir_subroutine *sub, mir_bb *bb,
                    mir_node_base *stmt) {
  switch (stmt->kind) {
  case MIR_NODE_BUILTIN: {
    mir_builtin *builtin = (mir_builtin *)stmt;
    switch (builtin->kind) {
    case MIR_BUILTIN_STORE: {
      cg_stmt_store(self, sub, bb, builtin);
    } break;
    case MIR_BUILTIN_DROP: {
      cg_stmt_drop(self, sub, bb, builtin);
    } break;
    default:
      error("unexpected builtin kind %d of stmt %p", builtin->kind, stmt);
    }
  } break;
  default:
    error("unexpected kind %d of stmt %p", stmt->kind, stmt);
    break;
  }
}

static void cg_block(cg_x86_64_ctx *self, mir_subroutine *sub, mir_bb *bb) {
  cg_x86_64_label *block_label =
      cg_x86_64_label_new(cg_block_name(sub, bb), CG_X86_64_LABEL_INSTR);
  list_cg_x86_64_unit_push_back(self->sub_units_ref,
                                (cg_x86_64_unit *)block_label);

  for (list_mir_node_it it = list_mir_node_begin(bb->stmts); !it.end(&it);
       it.next(&it)) {
    mir_node_base *stmt = it.get(&it);
    cg_stmt(self, sub, bb, stmt);
  }

  cg_jump(self, sub, bb, bb->jmp);
}

static char *cg_sub_name(mir_subroutine *sub, int global) {
  if (global) {
    return strdup(sub->name);
  } else {
    return cg_local_name(sub->name);
  }
}

static void cg_sub(cg_x86_64_ctx *self, mir_subroutine *sub, int exit,
                   int global) {
  self->sub_stack_size = X86_64_ALIGN_REG_64;
  self->sub_map_values = hashset_cg_x86_64_map_value_new();

  cg_x86_64_label *sub_label =
      cg_x86_64_label_new(cg_sub_name(sub, global), CG_X86_64_LABEL_INSTR);
  list_cg_x86_64_unit_push_back(self->sub_units_ref,
                                (cg_x86_64_unit *)sub_label);

  cg_sub_prologue(self);
  cg_value_prologue(self, sub);

  for (list_mir_bb_it it = list_mir_bb_begin(sub->bbs); !it.end(&it);
       it.next(&it)) {
    cg_block(self, sub, it.get(&it));
  }

  cg_sub_epilogue(self, sub, !exit);
  if (exit) {
    cg_sub_exit(self);
  }

  hashset_cg_x86_64_map_value_free(self->sub_map_values);
}

static void cg_units_append_move(list_cg_x86_64_unit *to,
                                 list_cg_x86_64_unit *from) {
  for (list_cg_x86_64_unit_it it = list_cg_x86_64_unit_begin(from);
       !it.end(&it); it.next(&it)) {
    list_cg_x86_64_unit_push_back(to, it.get(&it));
  }
  while (!list_cg_x86_64_unit_empty(from)) {
    list_cg_x86_64_unit_pop_front(from);
  }
  list_cg_x86_64_unit_free(from);
}

void cg_x86_64_ctx_build(cg_x86_64_ctx *self, mir *mir) {
  list_cg_x86_64_unit *units = self->cg_ref->units;

  cg_x86_64_dir *section_data = cg_x86_64_dir_section_new(strdup(".data"));
  list_cg_x86_64_unit *data_units = list_cg_x86_64_unit_new();
  self->data_units_ref = data_units;

  cg_x86_64_dir *section_text = cg_x86_64_dir_section_new(strdup(".text"));

  list_cg_x86_64_unit *globl_units = list_cg_x86_64_unit_new();

  list_cg_x86_64_unit *ext_units = list_cg_x86_64_unit_new();
  self->ext_units_ref = ext_units;

  self->globls = hashset_cg_x86_64_mir_ref_new();

  // core declarations
  for (int kind = 0; kind < X86_64_SYMBOL_UNKNOWN; ++kind) {
    list_cg_x86_64_unit_push_back(
        ext_units,
        (cg_x86_64_unit *)cg_x86_64_dir_extern_new(cg_x86_64_label_new(
            strdup(x86_64_symbol_enum_str(kind)), CG_X86_64_LABEL_DIRECTIVE)));
  }

  // executable declarations
  for (list_mir_subroutine_ext_it it =
           list_mir_subroutine_ext_begin(mir->sub_exts);
       !it.end(&it); it.next(&it)) {
    mir_subroutine_ext *ext = it.get(&it);
    char               *ext_name = strdup(ext->name);

    list_cg_x86_64_unit_push_back(
        ext_units,
        (cg_x86_64_unit *)cg_x86_64_dir_extern_new(
            cg_x86_64_label_new(ext_name, CG_X86_64_LABEL_DIRECTIVE)));

    hashset_cg_x86_64_mir_ref_insert(self->globls, (mir_node_base *)ext);
  }

  // add to globals
  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
       !it.end(&it); it.next(&it)) {
    mir_subroutine *sub = it.get(&it);

    if (!strcmp(sub->name, "main")) {
      list_cg_x86_64_unit_push_back(
          globl_units,
          (cg_x86_64_unit *)cg_x86_64_dir_globl_new(cg_x86_64_label_new(
              strdup(sub->name), CG_X86_64_LABEL_DIRECTIVE)));

      hashset_cg_x86_64_mir_ref_insert(self->globls, (mir_node_base *)sub);
    }
  }

  list_cg_x86_64_unit *text_units = list_cg_x86_64_unit_new();
  self->sub_units_ref = text_units;

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
       !it.end(&it); it.next(&it)) {
    mir_subroutine *sub = it.get(&it);

    int exit = cg_is_exit(self, sub);
    int global = cg_is_global(self, (mir_node_base *)sub);

    cg_sub(self, sub, exit, global);
  }

  // .section .data
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)section_data);
  cg_units_append_move(units, data_units);

  // .section .text
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)section_text);
  cg_units_append_move(units, ext_units);
  cg_units_append_move(units, globl_units);
  cg_units_append_move(units, text_units);

  hashset_cg_x86_64_mir_ref_free(self->globls);
}
