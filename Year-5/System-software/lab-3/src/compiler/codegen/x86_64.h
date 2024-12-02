#pragma once

#include "util/container_util.h"
#include "util/list.h"
#include <stdint.h>

typedef enum cg_x86_64_unit_enum {
  CG_X86_64_UNIT_UNKNOWN,
  CG_X86_64_UNIT_DATA,
  CG_X86_64_UNIT_INSTR,
  CG_X86_64_UNIT_LABEL,
  CG_X86_64_UNIT_DIRECTIVE,
} cg_x86_64_unit_enum;

typedef struct cg_x86_64_unit {
  cg_x86_64_unit_enum kind;
} cg_x86_64_unit;

void cg_x86_64_unit_free(cg_x86_64_unit *generic);

static inline void container_delete_cg_x86_64_unit(void *data) {
  cg_x86_64_unit_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_cg_x86_64_unit, cg_x86_64_unit,
                           container_cmp_false, container_new_move,
                           container_delete_cg_x86_64_unit);

typedef enum cg_x86_64_label_enum {
  CG_X86_64_LABEL_UNKNOWN,
  CG_X86_64_LABEL_DIRECTIVE,
  CG_X86_64_LABEL_DATA,
  CG_X86_64_LABEL_INSTR,
} cg_x86_64_label_enum;

typedef struct cg_x86_64_label_struct {
  cg_x86_64_unit       base;
  cg_x86_64_label_enum kind;
  char                *name;
} cg_x86_64_label;

cg_x86_64_label *cg_x86_64_label_new(char *name, cg_x86_64_label_enum kind);

typedef enum cg_x86_64_dir_enum {
  CG_X86_64_DIR_UNKNOWN,
  CG_X86_64_DIR_SECTION,
  CG_X86_64_DIR_GLOBL,
  CG_X86_64_DIR_EXTERN,
} cg_x86_64_dir_enum;

typedef struct cg_x86_64_dir_struct {
  cg_x86_64_unit     base;
  cg_x86_64_dir_enum kind;
  union {
    char            *section;
    cg_x86_64_label *label;
  };
} cg_x86_64_dir;

cg_x86_64_dir *cg_x86_64_dir_section_new(char *section);
cg_x86_64_dir *cg_x86_64_dir_globl_new(cg_x86_64_label *label);
cg_x86_64_dir *cg_x86_64_dir_extern_new(cg_x86_64_label *label);

typedef enum cg_x86_64_data_enum {
  CG_X86_64_DATA_BYTE,  // 1 byte
  CG_X86_64_DATA_WORD,  // 2 bytes
  CG_X86_64_DATA_LONG,  // 4 bytes
  CG_X86_64_DATA_QUAD,  // 8 bytes
  CG_X86_64_DATA_ASCII, // ascii string
} cg_x86_64_data_enum;

typedef struct cg_x86_64_data_struct {
  cg_x86_64_unit      base;
  cg_x86_64_data_enum kind;
  union {
    uint8_t  data_byte;
    uint16_t data_word;
    uint32_t data_long;
    uint64_t data_quad;
    uint8_t *data_ascii;
  };
} cg_x86_64_data;

cg_x86_64_data *cg_x86_64_data_new_byte(uint8_t data);
cg_x86_64_data *cg_x86_64_data_new_word(uint16_t data);
cg_x86_64_data *cg_x86_64_data_new_long(uint32_t data);
cg_x86_64_data *cg_x86_64_data_new_quad(uint64_t data);
cg_x86_64_data *cg_x86_64_data_new_ascii(uint8_t *data);

typedef enum cg_x86_64_reg_enum {
  CG_X86_64_REG_RAX,
  CG_X86_64_REG_RBX,
  CG_X86_64_REG_RCX,
  CG_X86_64_REG_RDX,
  CG_X86_64_REG_RSP,
  CG_X86_64_REG_RBP,
  CG_X86_64_REG_RSI,
  CG_X86_64_REG_RDI,
  CG_X86_64_REG_R8,
  CG_X86_64_REG_R9,
  CG_X86_64_REG_R10,
  CG_X86_64_REG_R11,
  CG_X86_64_REG_R12,
  CG_X86_64_REG_R13,
  CG_X86_64_REG_R14,
  CG_X86_64_REG_R15,
} cg_x86_64_reg;

// FINAL ADDRESS = ADDRESS_OR_OFFSET + %BASE_OR_OFFSET + MULTIPLIER * %INDEX
// ADDRESS_OR_OFFSET(%BASE_OR_OFFSET,%INDEX,MULTIPLIER)
//
// direct addressing:
// movl ADDRESS, %eax - load %eax with memory value at ADDRESS
// ADDRESS - constant or label
//
// indexed:
// movl ADDRESS(,%INDEX,MULTIPLIER), %eax
//
// indirect:
// movl (%BASE), %ebx
//
// base pointer:
// movl OFFSET(%BASE) - indirect + OFFSET
//
// immediate:
// movl $4, %ebx - address in now actual value
// $4 is ADDRESS (constant, 64 bit (hexed))
//
// currently supported modes:
typedef enum cg_x86_64_mode_enum {
  CG_X86_64_MODE_UNKNOWN,
  CG_X86_64_MODE_REGISTER,
  CG_X86_64_MODE_DIRECT,    // addr_label_ref(1)
  CG_X86_64_MODE_INDEXED,   // addr_label_ref(1) + index_reg(2) + multi_const(3)
  CG_X86_64_MODE_INDIRECT,  // base_reg(2)
  CG_X86_64_MODE_BASE,      // offset_const(1) + base_reg(2)
  CG_X86_64_MODE_IMMEDIATE, // imm_const(1)
} cg_x86_64_mode_enum;

typedef struct cg_x86_64_mode_struct {
  cg_x86_64_mode_enum kind;
  union {
    cg_x86_64_label *addr_label_ref;
    uint64_t         imm_const;
    uint64_t         offset_const;
    cg_x86_64_reg    reg;
  };
  union {
    cg_x86_64_reg index_reg;
    cg_x86_64_reg base_reg;
  };
  union {
    uint64_t multi_const;
  };
} cg_x86_64_mode;

cg_x86_64_mode *cg_x86_64_mode_new_register(cg_x86_64_reg reg);
cg_x86_64_mode *cg_x86_64_mode_new_direct(cg_x86_64_label *addr_label_ref);
cg_x86_64_mode *cg_x86_64_mode_new_indexed(cg_x86_64_label *addr_label_ref,
                                           cg_x86_64_reg    index_reg,
                                           uint64_t         multi_const);
cg_x86_64_mode *cg_x86_64_mode_new_indirect(cg_x86_64_reg base_reg);
cg_x86_64_mode *cg_x86_64_mode_new_base(uint64_t      offset_const,
                                        cg_x86_64_reg base_reg);
cg_x86_64_mode *cg_x86_64_mode_new_immediate(uint64_t imm_const);

void cg_x86_64_mode_free(cg_x86_64_mode *self);

static inline void container_delete_cg_x86_64_mode(void *data) {
  cg_x86_64_mode_free(data);
}
LIST_DECLARE_STATIC_INLINE(list_cg_x86_64_mode, cg_x86_64_mode,
                           container_cmp_false, container_new_move,
                           container_delete_cg_x86_64_mode);

typedef enum cg_x86_64_mnem_enum {
  CG_X86_64_MNEM_UNKNOWN,
  CG_X86_64_MNEM_PUSHQ,
  CG_X86_64_MNEM_POPQ,
  CG_X86_64_MNEM_MOVQ,
  CG_X86_64_MNEM_RETQ,
  CG_X86_64_MNEM_SYSCALL,
  CG_X86_64_MNEM_XORQ,
} cg_x86_64_mnem;

// at&t instruction order
typedef struct cg_x86_64_instr_struct {
  cg_x86_64_unit       base;
  cg_x86_64_mnem       mnem;
  list_cg_x86_64_mode *operands;
} cg_x86_64_instr;

cg_x86_64_instr *cg_x86_64_instr_new(cg_x86_64_mnem       mnem,
                                     list_cg_x86_64_mode *operands);

typedef struct cg_x86_64_struct {
  list_cg_x86_64_unit *units;
} cg_x86_64;

cg_x86_64 *cg_x86_64_new();
void       cg_x86_64_free(cg_x86_64 *self);
