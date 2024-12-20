# system-software-lab-4

## Dev-environment

Build:

```sh
docker build . -t zubrailx-system-software-lab-4
```

Run container and execute commands inside:

```sh
docker run -v .:/workspace -it zubrailx-system-software-lab-4:latest bash
```

Or run and connect with SSH:

```sh
docker run -v .:/workspace -itd zubrailx-system-software-lab-4:latest

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o GlobalKnownHostsFile=/dev/null \
    -p 2222 user@<ip>
```

- `ip` - container ip

Default password for user `user` is `user`.

Cd to mounted workspace:

```sh
cd /workspace
```

## Configuration

Make Targets:

```sh
make help
```
```
Targets:
  clean/build                 Remove build files
  clean/out                   Remove io output files
  compiler                    Build compiler executable
  debugger                    Build debugger executable
  format                      Format
  help                        Help
  index                       Index project
  run/asm                     Compile file
  run/dbg                     Run debugger
  run/pdf                     Run main executable and generate dot
  run/test                    Run all tests or specific
  stdc                        Build libstdc.a
  test                        Build tests
  util                        Build libutil.a
  x86_64_core                 Build libx86_64_core.a
Modules (prefixes of module targets):
  util compiler x86_64_core stdc debugger test
```

### Build compiler

```sh
make compiler
```

### Build debugger

```sh
make debugger
```

### Test

```sh
make run/test
```

### Run assembly

Run default suite:

```sh
make run/asm
```

Run selected suite (for example `io/suite8.txt`):

```sh
make run/asm/io/suite8.txt
```

Run manually:

```sh
./build/compiler/main  \
        -o io/suite8.txt.asm io/suite8.txt
as --64 -g -o io/suite8.txt.o io/suite8.txt.asm
ld -g -z noexecstack -o io/suite8.txt.out \
        -dynamic-linker /usr/lib64/ld-linux-x86-64.so.2 \
        /usr/lib/x86_64-linux-gnu/crt1.o \
        /usr/lib/x86_64-linux-gnu/crti.o \
        -lc \
        io/suite8.txt.o \
        /workspace/build/x86_64_core/libx86_64_core.a \
        /workspace/build/stdc/libstdc.a \
        /usr/lib/x86_64-linux-gnu/crtn.o
```

Help:

```sh
./build/compiler/main -h
```

```
Usage: ./build/compiler/main [options] <file>...
Options:
-d <directory>   - output directory (current: .)
-o <file>        - main output file (current: a.asm)
--tee            - print to file and to stdout (current: 0)
--ignore-errors  - continue execution on errors (current: 0)
--ast            - add AST output (current: 0)
--cfg            - add control flow graph output (current: 0)
--cfg-add-expr   - add expressions to control flow graph (current: 0)
--cg             - add call graph output (current: 0)
-s <subroutine>  - set or add subroutine for call graph generation(current: main)
--hir-symbols    - print HIR symbols (current: 0)
--hir-tree       - print HIR tree (current: 0)
--mir            - print MIR tree (current: 0)
-h               - show help
```

### Run debugger

Run with default init file:

```sh
make run/dbg
```

With specific init:

```sh
make run/dbg/io/dbg_init.txt
```

Run manually:

```sh
./build/debugger/main
```

Help:

```sh
./build/debugger/main -h
```

```
Usage: ./build/debugger/main [options] <file>...
Options:
-i <file>  - init file (current: (null))
-h         - show help
```
