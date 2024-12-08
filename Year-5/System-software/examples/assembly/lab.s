.section .data
size: .quad 64
.byte 0x15, 0x10

.section .text
.globl main

main:
	movq $60, %rax
	xor %rdi, %rdi
	syscall
