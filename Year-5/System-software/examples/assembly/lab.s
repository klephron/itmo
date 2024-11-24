.section .data
size: .quad 64

.section .text
.globl main
.extern __cg_x86_64_value_int
.extern __cg_x86_64_drop

main:
  movl $22, %edi
  call __cg_x86_64_value_int

  pushq %rax
  movq %rsp, %rdi
  call __cg_x86_64_drop

  addq $8, %rsp

	movq $60, %rax
	xor %rdi, %rdi
	syscall
