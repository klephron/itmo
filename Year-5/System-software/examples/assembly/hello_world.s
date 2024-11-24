.section .data
hw_str:
    .asciz "Hello world!"

.section .text
.globl main

main:
    # Function prologue
    pushq %rbp
    movq %rsp, %rbp

    # Pass the address of hw_str to puts
    leaq hw_str(%rip), %rdi  # Load effective address of hw_str into %rdi
    call puts                # Call puts

    # Set the return value to 0 (success)
    xor %rax, %rax           # Zero out %rax (return 0)

    # Function epilogue
    leave                    # Restore %rbp and %rsp
    ret                      # Return to caller
