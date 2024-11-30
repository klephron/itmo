#include "x86_64_ctx.h"

#include "util/log.h"
#include "util/macro.h"
#include <string.h>

typedef enum cg_reg_type_enum {
  CG_REG_UNKNOWN,
  CG_REG_CALLER,
  CG_REG_CALLEE,
} cg_reg_type;

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

static cg_reg_type cg_reg_is_caller(cg_x86_64_reg reg) {
  switch (reg) {
  case CG_X86_64_REG_RAX:
  case CG_X86_64_REG_RCX:
  case CG_X86_64_REG_RDX:
  case CG_X86_64_REG_RSI:
  case CG_X86_64_REG_RDI:
  case CG_X86_64_REG_R8:
  case CG_X86_64_REG_R9:
  case CG_X86_64_REG_R10:
  case CG_X86_64_REG_R11:
    return CG_REG_CALLER;
  case CG_X86_64_REG_RBX:
  case CG_X86_64_REG_RSP:
  case CG_X86_64_REG_RBP:
  case CG_X86_64_REG_R12:
  case CG_X86_64_REG_R13:
  case CG_X86_64_REG_R14:
  case CG_X86_64_REG_R15:
    return CG_REG_CALLEE;
  default:
    error("unknown reg %d", reg);
    return CG_REG_UNKNOWN;
  }
}

cg_x86_64_ctx *cg_x86_64_ctx_new(cg_x86_64 *cg) {
  cg_x86_64_ctx *self = MALLOC(cg_x86_64_ctx);
  self->cg = cg;
  self->exceptions = list_exception_new();
  return self;
}

void cg_x86_64_ctx_free(cg_x86_64_ctx *self) {
  if (self) {
    self->cg = NULL;
    list_exception_free(self->exceptions);
    free(self);
  }
}

void cg_x86_64_ctx_build(cg_x86_64_ctx *self, mir *mir) {
  UNUSED(self);
  UNUSED(mir);
}

char *cg_x86_64_ctx_emit(cg_x86_64_ctx *self) {
  UNUSED(self);
  return strdup(".section .data\n"
                "size: .quad 64\n"
                "\n"
                ".section .text\n"
                ".globl main\n"
                ".extern __cg_x86_64_value_int\n"
                ".section .text\n"
                ".extern __cg_x86_64_drop\n"
                "\n"
                "main:\n"
                "  movl $22, %edi\n"
                "  call __cg_x86_64_value_int\n"
                "\n"
                "  pushq %rax\n"
                "  movq %rsp, %rdi\n"
                "  call __cg_x86_64_drop\n"
                "\n"
                "  addq $8, %rsp\n"
                "\n"
                "	movq $60, %rax\n"
                "	xor %rdi, %rdi\n"
                "	syscall\n");
}
