# readme

## Assembly

* movq moves a quadword (64-bits) from source to destination.
* movl moves a long (32-bits) from source to destination.

objdump:

```sh
objdump -Mx86-64,att,amd64 -S build/exit.o
```

## GDB

```sh
gdb --tui
```

Press enter to repeat previous command.

In little endian format, the least significant byte (LSB) is stored at the lowest memory address, and the most significant byte (MSB) is stored at the highest memory address.

Network byte order - Big Endian.

Amd64 - little endian. Example:

```
0x12345678 -> 78 56 34 12

Memory Address    Data
0x00              0x78  (Least Significant Byte)
0x01              0x56
0x02              0x34
0x03              0x12  (Most Significant Byte)
```

Это же распространяется на quad и т.п.

When the movq instruction is used to load data from memory, it reads 8 bytes in the order specified by the address, starting from the lowest memory address and proceeding to higher memory addresses, not the other way around.

The bytes are read from left to right starting at the lowest memory address. The first byte loaded is from the lowest memory address (the first byte of the value), and it continues to the last byte of the 8-byte value.

movq 0, %rax

The 0 in this case is not the immediate value $0 (which would mean the value 0), but rather the address 0 in memory, meaning you are trying to load data from memory address 0.

```
# load value array value
movl string_start(,%ecx,1), %eax

# indirect address
movl (%eax), %ebx

# base pointer (with offset)
movl 4(%eax), %ebx

# immediate
movl $12, %eax

# registers
movl %eax, %ebx

# load value at address passed as value
mov edi, [0xdeadbeef] ; Intel
movl 0xdeadbeef, %edi ; AT&T
```
