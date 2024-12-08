#include "compiler/codegen/x86_64.h"
#include "util/log.h"
#include "util/macro.h"
#include <string.h>

void cg_x86_64_label_free(cg_x86_64_label *self);
void cg_x86_64_data_free(cg_x86_64_data *self);
void cg_x86_64_instr_free(cg_x86_64_instr *self);
void cg_x86_64_dir_free(cg_x86_64_dir *self);

// GENERIC
static inline void cg_x86_64_unit_base_init(cg_x86_64_unit     *base,
                                            cg_x86_64_unit_enum kind) {
  base->kind = kind;
}

static inline void cg_x86_64_unit_base_deinit(cg_x86_64_unit *base) {
  UNUSED(base);
}

void cg_x86_64_unit_free(cg_x86_64_unit *generic) {
  if (!generic) {
    return;
  }
  switch (generic->kind) {
  case CG_X86_64_UNIT_DATA:
    return cg_x86_64_data_free((cg_x86_64_data *)generic);
  case CG_X86_64_UNIT_INSTR:
    return cg_x86_64_instr_free((cg_x86_64_instr *)generic);
  case CG_X86_64_UNIT_LABEL:
    return cg_x86_64_label_free((cg_x86_64_label *)generic);
  case CG_X86_64_UNIT_DIRECTIVE:
    return cg_x86_64_dir_free((cg_x86_64_dir *)generic);
  case CG_X86_64_UNIT_UNKNOWN:
  default:
    error("unknown assembly unit %d %p", generic->kind, generic);
    break;
  }
}

// LABEL
cg_x86_64_label *cg_x86_64_label_new(char *name, cg_x86_64_label_enum kind) {
  cg_x86_64_label *self = MALLOC(cg_x86_64_label);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_LABEL);
  self->name = name;
  self->kind = kind;
  return self;
}

void cg_x86_64_label_free(cg_x86_64_label *self) {
  if (self) {
    free(self->name);
    cg_x86_64_unit_base_deinit(&self->base);
    free(self);
  }
}

// DIRECTIVE
cg_x86_64_dir *cg_x86_64_dir_section_new(char *section) {
  cg_x86_64_dir *self = MALLOC(cg_x86_64_dir);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DIRECTIVE);
  self->kind = CG_X86_64_DIR_SECTION;
  self->section = section;
  return self;
}

cg_x86_64_dir *cg_x86_64_dir_globl_new(cg_x86_64_label *label) {
  cg_x86_64_dir *self = MALLOC(cg_x86_64_dir);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DIRECTIVE);
  self->kind = CG_X86_64_DIR_GLOBL;
  self->label = label;
  return self;
}

cg_x86_64_dir *cg_x86_64_dir_extern_new(cg_x86_64_label *label) {
  cg_x86_64_dir *self = MALLOC(cg_x86_64_dir);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DIRECTIVE);
  self->kind = CG_X86_64_DIR_EXTERN;
  self->label = label;
  return self;
}

void cg_x86_64_dir_free(cg_x86_64_dir *self) {
  if (self) {
    switch (self->kind) {
    case CG_X86_64_DIR_SECTION:
      free(self->section);
      break;
    case CG_X86_64_DIR_GLOBL:
    case CG_X86_64_DIR_EXTERN:
      cg_x86_64_label_free(self->label);
      break;
    default:
      break;
    }
    free(self);
  }
}

// DATA
cg_x86_64_data *cg_x86_64_data_new_byte(uint8_t data) {
  cg_x86_64_data *self = MALLOC(cg_x86_64_data);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DATA);
  self->kind = CG_X86_64_DATA_BYTE;
  self->data_byte = data;
  self->data_size = sizeof(data);
  return self;
}

cg_x86_64_data *cg_x86_64_data_new_word(uint16_t data) {
  cg_x86_64_data *self = MALLOC(cg_x86_64_data);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DATA);
  self->kind = CG_X86_64_DATA_WORD;
  self->data_word = data;
  self->data_size = sizeof(data);
  return self;
}

cg_x86_64_data *cg_x86_64_data_new_long(uint32_t data) {
  cg_x86_64_data *self = MALLOC(cg_x86_64_data);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DATA);
  self->kind = CG_X86_64_DATA_LONG;
  self->data_long = data;
  self->data_size = sizeof(data);
  return self;
}

cg_x86_64_data *cg_x86_64_data_new_quad(uint64_t data) {
  cg_x86_64_data *self = MALLOC(cg_x86_64_data);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DATA);
  self->kind = CG_X86_64_DATA_QUAD;
  self->data_quad = data;
  self->data_size = sizeof(data);
  return self;
}

cg_x86_64_data *cg_x86_64_data_new_ascii(uint8_t *data) {
  cg_x86_64_data *self = MALLOC(cg_x86_64_data);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DATA);
  self->kind = CG_X86_64_DATA_ASCII;
  self->data_ascii = data;
  self->data_size = strlen((char *)data) + 1;
  return self;
}

cg_x86_64_data *cg_x86_64_data_new_bytes(size_t size, uint8_t *data) {
  cg_x86_64_data *self = MALLOC(cg_x86_64_data);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_DATA);
  self->kind = CG_X86_64_DATA_BYTES;
  self->data_bytes = data;
  self->data_size = size;
  return self;
}

void cg_x86_64_data_free(cg_x86_64_data *self) {
  if (self) {
    switch (self->kind) {
    case CG_X86_64_DATA_ASCII:
      free(self->data_ascii);
      break;
    case CG_X86_64_DATA_BYTES:
      free(self->data_bytes);
      break;
    case CG_X86_64_DATA_BYTE:
    case CG_X86_64_DATA_WORD:
    case CG_X86_64_DATA_LONG:
    case CG_X86_64_DATA_QUAD:
      break;
    default:
      error("unknown data kind %d %p", self->kind, self);
      return;
    }
    cg_x86_64_unit_base_deinit(&self->base);
    free(self);
  }
}

// INSTRUCTION
cg_x86_64_op *cg_x86_64_op_new_register(cg_x86_64_reg reg) {
  cg_x86_64_op *self = MALLOC(cg_x86_64_op);
  self->kind = CG_X86_64_MODE_REGISTER;
  self->reg = reg;
  return self;
}
cg_x86_64_op *cg_x86_64_op_new_direct(char *addr_label) {
  cg_x86_64_op *self = MALLOC(cg_x86_64_op);
  self->kind = CG_X86_64_MODE_DIRECT;
  self->addr_label = addr_label;
  return self;
}

cg_x86_64_op *cg_x86_64_op_new_indexed(char         *addr_label,
                                       cg_x86_64_reg index_reg,
                                       uint64_t      multi_const) {
  cg_x86_64_op *self = MALLOC(cg_x86_64_op);
  self->kind = CG_X86_64_MODE_INDEXED;
  self->addr_label = addr_label;
  self->index_reg = index_reg;
  self->multi_const = multi_const;
  return self;
}

cg_x86_64_op *cg_x86_64_op_new_indirect(cg_x86_64_reg base_reg) {
  cg_x86_64_op *self = MALLOC(cg_x86_64_op);
  self->kind = CG_X86_64_MODE_INDIRECT;
  self->base_reg = base_reg;
  return self;
}

cg_x86_64_op *cg_x86_64_op_new_base(uint64_t      offset_const,
                                    cg_x86_64_reg base_reg) {
  cg_x86_64_op *self = MALLOC(cg_x86_64_op);
  self->kind = CG_X86_64_MODE_BASE;
  self->offset_const = offset_const;
  self->base_reg = base_reg;
  return self;
}

cg_x86_64_op *cg_x86_64_op_new_immediate(uint64_t imm_const) {
  cg_x86_64_op *self = MALLOC(cg_x86_64_op);
  self->kind = CG_X86_64_MODE_IMMEDIATE;
  self->imm_const = imm_const;
  return self;
}

void cg_x86_64_op_free(cg_x86_64_op *self) {
  if (self) {
    switch (self->kind) {
    case CG_X86_64_MODE_DIRECT:
    case CG_X86_64_MODE_INDEXED:
      free(self->addr_label);
      break;
    default:
      break;
    }
    free(self);
  }
}

cg_x86_64_instr *cg_x86_64_instr_new(cg_x86_64_mnem     mnem,
                                     list_cg_x86_64_op *operands) {
  cg_x86_64_instr *self = MALLOC(cg_x86_64_instr);
  cg_x86_64_unit_base_init(&self->base, CG_X86_64_UNIT_INSTR);
  self->mnem = mnem;
  self->operands = operands;
  return self;
}

void cg_x86_64_instr_free(cg_x86_64_instr *self) {
  if (self) {
    list_cg_x86_64_op_free(self->operands);
    cg_x86_64_unit_base_deinit(&self->base);
    free(self);
  }
}

// CODEGEN MAIN
cg_x86_64 *cg_x86_64_new() {
  cg_x86_64 *self = MALLOC(cg_x86_64);
  self->units = list_cg_x86_64_unit_new();
  return self;
}

void cg_x86_64_free(cg_x86_64 *self) {
  if (self) {
    list_cg_x86_64_unit_free(self->units);
    free(self);
  }
}
