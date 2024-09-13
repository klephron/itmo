# SPPO

## Build

### Using docker

```sh
docker build . -t <tag>
```

### Using devpod-cli (devcontainers)

```sh
devpod-cli up . --ide none [--recreate]
```

## Start

Mount this directory on `/workspaces/sppo`.

```sh
docker run -v .:/workspaces/sppo -d -it <tag> bash
```

## Connect

```sh
ssh -p 2222 'root@<ip>'
```

Default password is:

```sh
root
```

Get ip:

```sh
docker inspect -f "{{ .Name}} {{ .NetworkSettings.IPAddress }} <container-id>"
```

