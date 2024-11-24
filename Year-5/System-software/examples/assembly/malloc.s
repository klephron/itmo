.section .data
size: .quad 64

.section .text
.globl main
.extern malloc
.extern free
.extern example_func
.extern example2_func

main:
  call example_func
  call example2_func

	movq size(%rip), %rdi

	call malloc

	testq %rax, %rax
	jz allocation_failed

	movq %rax, %rdi

	call free

	movq $60, %rax
	xor %rdi, %rdi
	syscall

allocation_failed:
	movq $60, %rax
	movq $1, %rdi
	syscall
