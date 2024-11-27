#include "compiler/codegen/x86_64.h"
#include "util/macro.h"
#include <string.h>

void cg_x86_64_free(cg_x86_64 *self) {
  if (self) {
    free(self);
  }
}

cg_x86_64 *cg_x86_64_build(mir *mir) {
  UNUSED(mir);
  cg_x86_64 *self = MALLOC(cg_x86_64);
  return self;
}

char *cg_x86_64_emit(cg_x86_64 *self) {
  UNUSED(self);
  return strdup(".section .data\n"
                "size: .quad 64\n"
                "\n"
                ".section .text\n"
                ".globl main\n"
                ".extern __cg_x86_64_value_int\n"
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
