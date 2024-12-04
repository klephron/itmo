#include "x86_64_ctx.h"

#include "x86_64_core/x86_64_core.h"
#include "x86_64_core/x86_64_meta.h"

#include <string.h>

typedef enum cg_reg_type_enum {
  CG_REG_UNKNOWN,
  CG_REG_CALLER,
  CG_REG_CALLEE,
} cg_reg_type;

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

static void cg_sub_epilogue(cg_x86_64_ctx *self, int ret) {
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
        self->sub_map_value, cg_x86_64_map_value_new(sub->ret, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->args); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        self->sub_map_value, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->vars); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        self->sub_map_value, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }

  for (list_mir_value_it it = list_mir_value_begin(sub->tmps); !it.end(&it);
       it.next(&it)) {
    mir_value *value = it.get(&it);
    hashset_cg_x86_64_map_value_insert(
        self->sub_map_value, cg_x86_64_map_value_new(value, rbp_offset));
    rbp_offset += sizeof(x86_64_value);
  }
}

static void cg_subroutine(cg_x86_64_ctx *self, mir_subroutine *sub, int exit) {
  self->sub_stack_size = X86_64_ALIGN_REG_64;
  self->sub_map_value = hashset_cg_x86_64_map_value_new();

  cg_x86_64_label *sub_label =
      cg_x86_64_label_new(strdup(sub->name), CG_X86_64_LABEL_INSTR);
  list_cg_x86_64_unit_push_back(self->sub_units_ref,
                                (cg_x86_64_unit *)sub_label);

  cg_sub_prologue(self);
  cg_value_prologue(self, sub);

  cg_sub_epilogue(self, !exit);

  if (exit) {
    cg_sub_exit(self);
  }
  hashset_cg_x86_64_map_value_free(self->sub_map_value);
}

void cg_x86_64_ctx_build(cg_x86_64_ctx *self, mir *mir) {
  list_cg_x86_64_unit *units = self->cg_ref->units;

  cg_x86_64_dir *section_data = cg_x86_64_dir_section_new(strdup(".data"));
  list_cg_x86_64_unit *data_units = list_cg_x86_64_unit_new();
  self->data_units_ref = data_units;

  cg_x86_64_dir *section_text = cg_x86_64_dir_section_new(strdup(".text"));
  cg_x86_64_dir *globl_main = cg_x86_64_dir_globl_new(
      cg_x86_64_label_new(strdup("main"), CG_X86_64_LABEL_DIRECTIVE));

  list_cg_x86_64_unit *ext_units = list_cg_x86_64_unit_new();
  self->ext_units_ref = ext_units;

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
    list_cg_x86_64_unit_push_back(
        ext_units,
        (cg_x86_64_unit *)cg_x86_64_dir_extern_new(
            cg_x86_64_label_new(strdup(ext->name), CG_X86_64_LABEL_DIRECTIVE)));
  }

  list_cg_x86_64_unit *text_units = list_cg_x86_64_unit_new();
  self->sub_units_ref = text_units;

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
       !it.end(&it); it.next(&it)) {
    mir_subroutine *sub = it.get(&it);
    if (!strcmp(sub->name, "main")) {
      cg_subroutine(self, sub, 1);
    } else {
      cg_subroutine(self, sub, 0);
    }
  }

  // .section .data
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)section_data);

  for (list_cg_x86_64_unit_it it = list_cg_x86_64_unit_begin(data_units);
       !it.end(&it); it.next(&it)) {
    list_cg_x86_64_unit_push_back(units, it.get(&it));
  }
  while (!list_cg_x86_64_unit_empty(data_units)) {
    list_cg_x86_64_unit_pop_front(data_units);
  }
  list_cg_x86_64_unit_free(data_units);

  // .section .text
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)section_text);

  // .extern directive...
  for (list_cg_x86_64_unit_it it = list_cg_x86_64_unit_begin(ext_units);
       !it.end(&it); it.next(&it)) {
    list_cg_x86_64_unit_push_back(units, it.get(&it));
  }
  while (!list_cg_x86_64_unit_empty(ext_units)) {
    list_cg_x86_64_unit_pop_front(ext_units);
  }
  list_cg_x86_64_unit_free(ext_units);

  // .globl directive...
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)globl_main);

  // subroutines...
  for (list_cg_x86_64_unit_it it = list_cg_x86_64_unit_begin(text_units);
       !it.end(&it); it.next(&it)) {
    list_cg_x86_64_unit_push_back(units, it.get(&it));
  }
  while (!list_cg_x86_64_unit_empty(text_units)) {
    list_cg_x86_64_unit_pop_front(text_units);
  }
  list_cg_x86_64_unit_free(text_units);
}
