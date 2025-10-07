# pa7

## Build

```sh
make build
```

## Run

```sh
make run -- <args...>
```

> `<args...>` - args that needs to be passed to executable

E.g. run with 10 processes:

```sh
make run -- -p 10
```

## Format

```sh
make format
```

## Clean

```sh
make clean
```

## Docker

To build and run above `make` commands docker environment can be used.

Build:

```sh
docker build -t klephron/itmo-dc .
```

Run:

```sh
docker run -v .:/app -it klephron/itmo-dc:latest bash
```

To use `make format` replace `FORMAT` in `Makefile`:

```make
FORMAT       ?= clang-format -i
```
