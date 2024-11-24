# readme

## Assembly

* movq moves a quadword (64-bits) from source to destination.
* movl moves a long (32-bits) from source to destination.

objdump:

```sh
objdump -Mx86-64,att,amd64 -S build/exit.o
```
