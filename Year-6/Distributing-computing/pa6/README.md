# pa6

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

## Clean

```sh
make clean
```

## Docker

To build and run make commands above docker environment can be used.

Build:

```sh
docker build -t klephron/itmo-dc .
```

Run:

```sh
docker run -v .:/app -it klephron/itmo-dc:latest bash
```
