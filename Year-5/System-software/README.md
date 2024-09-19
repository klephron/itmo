# SPPO

`Makefile` is hardlinked among all the projects.

## Build

### Using docker

```sh
docker build . -t <tag>
```

### Using devpod-cli (devcontainers)

```sh
devpod-cli up . --ide none [--recreate]
```

Default remote user is `vscode`. ID of this user is used to mount workspace.

## Start

Mount this directory on `/workspaces/system-software`.

```sh
docker run -v .:/workspaces/system-software -d -it <tag> bash
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

> Or use user `vscode` when used with devcontainers.

