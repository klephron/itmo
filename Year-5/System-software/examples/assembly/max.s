.section .data
  data_items:
    .quad 300000000005,67,34,222,45,75,54,34,44,33,22,11,66,0

.section .text
.globl _start

_start:
  movq (0), %rdi # move 0 into the index register
  movq data_items(,%rdi,8), %rax # load the first byte of data
  movq %rax, %rbx # since this is the first item, %eax is the biggest

start_loop: # start loop
  cmpq $0, %rax # check to see if we’ve hit the end
  je loop_exit
  incq %rdi # load next value
  movq data_items(,%rdi,8), %rax
  cmpq %rbx, %rax # compare values
  jle start_loop
  movq %rax, %rbx # move the value as the largest
  jmp start_loop

loop_exit:
# %ebx is the status code for the exit system call
# and it already has the maximum number
  movq $1, %rax #1 is the exit() syscall
  int $0x80
