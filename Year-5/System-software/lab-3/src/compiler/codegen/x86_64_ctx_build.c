#include "x86_64_ctx.h"

#include "util/log.h"
#include "util/macro.h"
#include <string.h>

typedef enum cg_reg_type_enum {
  CG_REG_UNKNOWN,
  CG_REG_CALLER,
  CG_REG_CALLEE,
} cg_reg_type;

static void cg_add_prologue(cg_x86_64_ctx *self, list_cg_x86_64_unit *units) {
  UNUSED(self);
  list_cg_x86_64_mode *operands;

  // pushq %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                           CG_X86_64_MNEM_PUSHQ, operands));
  // movq %rsp, %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RSP));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                           CG_X86_64_MNEM_MOVQ, operands));
}

static void cg_add_epilogue(cg_x86_64_ctx *self, list_cg_x86_64_unit *units,
                            int ret) {
  UNUSED(self);
  list_cg_x86_64_mode *operands;
  // movq %rsp, %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RSP));
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                           CG_X86_64_MNEM_MOVQ, operands));
  // popq %rbp
  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RBP));
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                           CG_X86_64_MNEM_POPQ, operands));

  // ret
  if (ret) {
    operands = list_cg_x86_64_mode_new();
    list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                             CG_X86_64_MNEM_RETQ, operands));
  }
}

static void cg_exit(cg_x86_64_ctx *self, list_cg_x86_64_unit *units) {
  UNUSED(self);
  list_cg_x86_64_mode *operands;

  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands, cg_x86_64_mode_new_immediate(60));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RAX));
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                           CG_X86_64_MNEM_MOVQ, operands));

  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RDI));
  list_cg_x86_64_mode_push_back(operands,
                                cg_x86_64_mode_new_register(CG_X86_64_REG_RDI));
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                           CG_X86_64_MNEM_XORQ, operands));

  operands = list_cg_x86_64_mode_new();
  list_cg_x86_64_unit_push_back(units, (cg_x86_64_unit *)cg_x86_64_instr_new(
                                           CG_X86_64_MNEM_SYSCALL, operands));
}

void cg_x86_64_ctx_build_subroutine(cg_x86_64_ctx       *self,
                                    list_cg_x86_64_unit *data_units,
                                    list_cg_x86_64_unit *text_units,
                                    mir_subroutine *sub, int exit) {
  list_cg_x86_64_mode *operands;

  cg_x86_64_label *sub_label =
      cg_x86_64_label_new(strdup(sub->name), CG_X86_64_LABEL_INSTR);
  list_cg_x86_64_unit_push_back(text_units, (cg_x86_64_unit *)sub_label);

  cg_add_prologue(self, text_units);
  cg_add_epilogue(self, text_units, !exit);

  if (exit) {
    cg_exit(self, text_units);
  }
}

void cg_x86_64_ctx_build(cg_x86_64_ctx *self, mir *mir) {
  list_cg_x86_64_unit *units = self->cg_ref->units;

  cg_x86_64_dir *section_data = cg_x86_64_dir_section_new(strdup(".data"));
  cg_x86_64_dir *section_text = cg_x86_64_dir_section_new(strdup(".text"));
  cg_x86_64_dir *globl_main = cg_x86_64_dir_globl_new(
      cg_x86_64_label_new(strdup("main"), CG_X86_64_LABEL_DIRECTIVE));

  list_cg_x86_64_unit *exts = list_cg_x86_64_unit_new();
  for (list_mir_subroutine_ext_it it =
           list_mir_subroutine_ext_begin(mir->sub_exts);
       !it.end(&it); it.next(&it)) {
    mir_subroutine_ext *ext = it.get(&it);
    list_cg_x86_64_unit_push_back(
        exts, (cg_x86_64_unit *)cg_x86_64_dir_extern_new(cg_x86_64_label_new(
                  strdup(ext->name), CG_X86_64_LABEL_DIRECTIVE)));
  }

  list_cg_x86_64_unit *data_units = list_cg_x86_64_unit_new();
  list_cg_x86_64_unit *text_units = list_cg_x86_64_unit_new();

  for (list_mir_subroutine_it it = list_mir_subroutine_begin(mir->subs);
       !it.end(&it); it.next(&it)) {
    mir_subroutine *sub = it.get(&it);
    if (!strcmp(sub->name, "main")) {
      cg_x86_64_ctx_build_subroutine(self, data_units, text_units, sub, 1);
    } else {
      cg_x86_64_ctx_build_subroutine(self, data_units, text_units, sub, 0);
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
  for (list_cg_x86_64_unit_it it = list_cg_x86_64_unit_begin(exts);
       !it.end(&it); it.next(&it)) {
    list_cg_x86_64_unit_push_back(units, it.get(&it));
  }
  while (!list_cg_x86_64_unit_empty(exts)) {
    list_cg_x86_64_unit_pop_front(exts);
  }
  list_cg_x86_64_unit_free(exts);

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
