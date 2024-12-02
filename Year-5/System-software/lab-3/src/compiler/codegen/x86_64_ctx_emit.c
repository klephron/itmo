#include "x86_64_ctx.h"

#include "util/log.h"
#include "util/macro.h"
#include "util/string_buffer.h"
#include <stdio.h>

static const char *cg_reg_str(cg_x86_64_reg reg) {
  switch (reg) {
  case CG_X86_64_REG_RAX:
    return "rax";
  case CG_X86_64_REG_RBX:
    return "rbx";
  case CG_X86_64_REG_RCX:
    return "rcx";
  case CG_X86_64_REG_RDX:
    return "rdx";
  case CG_X86_64_REG_RSP:
    return "rsp";
  case CG_X86_64_REG_RBP:
    return "rbp";
  case CG_X86_64_REG_RSI:
    return "rsi";
  case CG_X86_64_REG_RDI:
    return "rdi";
  case CG_X86_64_REG_R8:
    return "r8";
  case CG_X86_64_REG_R9:
    return "r9";
  case CG_X86_64_REG_R10:
    return "r10";
  case CG_X86_64_REG_R11:
    return "r11";
  case CG_X86_64_REG_R12:
    return "r12";
  case CG_X86_64_REG_R13:
    return "r13";
  case CG_X86_64_REG_R14:
    return "r14";
  case CG_X86_64_REG_R15:
    return "r15";
  default:
    error("unknown reg kind %d", reg);
    return "unknown";
    break;
  }
}

static void cg_emit_mode(cg_x86_64_ctx *self, string_buffer *buf,
                         cg_x86_64_mode *op) {
  UNUSED(self);
  char sbuf[32];

  switch (op->kind) {
  case CG_X86_64_MODE_REGISTER:
    string_buffer_append(buf, "%");
    string_buffer_append(buf, cg_reg_str(op->reg));
    break;
  case CG_X86_64_MODE_DIRECT:
    string_buffer_append(buf, op->addr_label_ref->name);
    break;
  case CG_X86_64_MODE_INDEXED:
    string_buffer_append(buf, op->addr_label_ref->name);
    string_buffer_append(buf, "(");
    string_buffer_append(buf, ",%");
    string_buffer_append(buf, cg_reg_str(op->base_reg));
    string_buffer_append(buf, ",");
    snprintf(sbuf, STRMAXLEN(sbuf), "%#lx", op->multi_const);
    string_buffer_append(buf, sbuf);
    string_buffer_append(buf, ")");
    break;
  case CG_X86_64_MODE_INDIRECT:
    string_buffer_append(buf, "(%");
    string_buffer_append(buf, cg_reg_str(op->base_reg));
    string_buffer_append(buf, ")");
    break;
  case CG_X86_64_MODE_BASE:
    snprintf(sbuf, STRMAXLEN(sbuf), "%#lx", op->offset_const);
    string_buffer_append(buf, sbuf);
    string_buffer_append(buf, "(%");
    string_buffer_append(buf, cg_reg_str(op->base_reg));
    string_buffer_append(buf, ")");
    break;
  case CG_X86_64_MODE_IMMEDIATE:
    string_buffer_append(buf, "$");
    snprintf(sbuf, STRMAXLEN(sbuf), "%#lx", op->imm_const);
    string_buffer_append(buf, sbuf);
    break;
  default:
    error("unexpected mode kind %d %p", op->kind, op);
    break;
  }
}

static void cg_emit_data(cg_x86_64_ctx *self, string_buffer *buf,
                         cg_x86_64_data *data) {
  UNUSED(self);
  char sbuf[32];

  switch (data->kind) {
  case CG_X86_64_DATA_BYTE:
    string_buffer_append(buf, ".byte ");
    snprintf(sbuf, STRMAXLEN(sbuf), "%#x", data->data_byte);
    string_buffer_append(buf, sbuf);
    break;
  case CG_X86_64_DATA_WORD:
    string_buffer_append(buf, ".word ");
    snprintf(sbuf, STRMAXLEN(sbuf), "%#x", data->data_word);
    string_buffer_append(buf, sbuf);
    break;
  case CG_X86_64_DATA_LONG:
    string_buffer_append(buf, ".long ");
    snprintf(sbuf, STRMAXLEN(sbuf), "%#x", data->data_long);
    string_buffer_append(buf, sbuf);
    break;
  case CG_X86_64_DATA_QUAD:
    string_buffer_append(buf, ".quad ");
    snprintf(sbuf, STRMAXLEN(sbuf), "%#lx", data->data_quad);
    string_buffer_append(buf, sbuf);
    break;
  case CG_X86_64_DATA_ASCII:
    string_buffer_append(buf, ".ascii");
    string_buffer_append(buf, "\"");
    string_buffer_append(buf, (char *)data->data_ascii);
    string_buffer_append(buf, "\\0\"");
    break;
  default:
    error("unexpected data kind %d %p", data->kind, data);
  }
  string_buffer_append(buf, "\n");
}

void cg_emit_instr(cg_x86_64_ctx *self, string_buffer *buf,
                   cg_x86_64_instr *instr) {
  list_cg_x86_64_mode *operands = instr->operands;
  string_buffer_append(buf, "  ");
  switch (instr->mnem) {
  case CG_X86_64_MNEM_PUSHQ:
    string_buffer_append(buf, "pushq");
    break;
  case CG_X86_64_MNEM_POPQ:
    string_buffer_append(buf, "popq");
    break;
  case CG_X86_64_MNEM_MOVQ:
    string_buffer_append(buf, "movq");
    break;
  case CG_X86_64_MNEM_RETQ:
    string_buffer_append(buf, "retq");
    break;
  case CG_X86_64_MNEM_XORQ:
    string_buffer_append(buf, "xorq");
    break;
  case CG_X86_64_MNEM_SYSCALL:
    string_buffer_append(buf, "syscall");
    break;
  case CG_X86_64_MNEM_UNKNOWN:
  default:
    error("unexpected instr kind %d %p", instr->mnem, instr);
    break;
  }
  list_cg_x86_64_mode_it op_it = list_cg_x86_64_mode_begin(operands);
  if (!op_it.end(&op_it)) {
    string_buffer_append(buf, " ");
    cg_emit_mode(self, buf, op_it.get(&op_it));
    op_it.next(&op_it);
  }
  for (; !op_it.end(&op_it); op_it.next(&op_it)) {
    string_buffer_append(buf, ", ");
    cg_emit_mode(self, buf, op_it.get(&op_it));
  }
  string_buffer_append(buf, "\n");
}

void cg_emit_label(cg_x86_64_ctx *self, string_buffer *buf,
                   cg_x86_64_label *label) {
  UNUSED(self);

  switch (label->kind) {
  case CG_X86_64_LABEL_DATA:
    string_buffer_append(buf, label->name);
    string_buffer_append(buf, ": ");
    break;
  case CG_X86_64_LABEL_INSTR:
    string_buffer_append(buf, label->name);
    string_buffer_append(buf, ":\n");
    break;
  case CG_X86_64_LABEL_DIRECTIVE:
    info("unexpected label directive %d %p", label->kind, label);
    break;
  default:
    error("unknown label kind %d %p", label->kind, label);
    break;
  }
}

void cg_emit_directive(cg_x86_64_ctx *self, string_buffer *buf,
                       cg_x86_64_dir *dir) {
  UNUSED(self);

  switch (dir->kind) {
  case CG_X86_64_DIR_SECTION:
    string_buffer_append(buf, ".section ");
    string_buffer_append(buf, dir->section);
    break;
  case CG_X86_64_DIR_GLOBL:
    string_buffer_append(buf, ".globl ");
    string_buffer_append(buf, dir->label->name);
    break;
  case CG_X86_64_DIR_EXTERN:
    string_buffer_append(buf, ".extern ");
    string_buffer_append(buf, dir->label->name);
    break;
  case CG_X86_64_DIR_UNKNOWN:
  default:
    error("unknown directive kind %d %p", dir->kind, dir);
    break;
  }
  string_buffer_append(buf, "\n");
}

char *cg_x86_64_ctx_emit(cg_x86_64_ctx *self) {

  string_buffer *buf = string_buffer_new(0, 0);

  for (list_cg_x86_64_unit_it it =
           list_cg_x86_64_unit_begin(self->cg_ref->units);
       !it.end(&it); it.next(&it)) {
    cg_x86_64_unit *generic = it.get(&it);

    switch (generic->kind) {
    case CG_X86_64_UNIT_DATA:
      cg_emit_data(self, buf, (cg_x86_64_data *)generic);
      break;
    case CG_X86_64_UNIT_INSTR: {
      cg_emit_instr(self, buf, (cg_x86_64_instr *)generic);
    } break;
    case CG_X86_64_UNIT_LABEL: {
      cg_emit_label(self, buf, (cg_x86_64_label *)generic);
    } break;
    case CG_X86_64_UNIT_DIRECTIVE: {
      cg_emit_directive(self, buf, (cg_x86_64_dir *)generic);
    } break;
    case CG_X86_64_UNIT_UNKNOWN:
    default:
      error("unknown unit %d %p", generic->kind, generic);
    }
  }

  return string_buffer_detach(buf);
  // return strdup(".section .data\n"
  //               "size: .quad 64\n"
  //               "\n"
  //               ".section .text\n"
  //               ".globl main\n"
  //               ".extern __cg_x86_64_value_int\n"
  //               ".section .text\n"
  //               ".extern __cg_x86_64_drop\n"
  //               "\n"
  //               "main:\n"
  //               "  movl $22, %edi\n"
  //               "  call __cg_x86_64_value_int\n"
  //               "\n"
  //               "  pushq %rax\n"
  //               "  movq %rsp, %rdi\n"
  //               "  call __cg_x86_64_drop\n"
  //               "\n"
  //               "  addq $8, %rsp\n"
  //               "\n"
  //               "	movq $60, %rax\n"
  //               "	xor %rdi, %rdi\n"
  //               "	syscall\n");
}
