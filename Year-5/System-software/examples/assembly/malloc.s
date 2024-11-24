.section .data
size: .long 64

.section .text
.globl main
.extern malloc
.extern free

main:
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
