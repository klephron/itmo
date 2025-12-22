# gitlab-runner

## Registration

Register (e.g. for `brainstop-runner-1`):

```sh
docker exec -it brainstop-runner-1 gitlab-runner register --help
```

### Frontend

```sh
docker exec -it brainstop-runner-1 gitlab-runner register --non-interactive \
    --url https://gitlab.se.ifmo.ru/ \
    --registration-token $REGISTRATION_TOKEN_FRONTEND \
    --name "brainstop-runner-1" \
    --tag-list "brainstop-docker" \
    --run-untagged \
    --executor docker \
    --docker-image "ubuntu:24.04" \
    --docker-network-mode "bridge" \
    --docker-pull-policy "if-not-present" \
    --docker-privileged \
    --env "DOCKER_HOST=unix:///var/run/docker.sock" \
    --docker-volumes "/var/run/docker.sock:/var/run/docker.sock" \
```

### Backend

```sh
docker exec -it brainstop-runner-1 gitlab-runner register --non-interactive \
    --url https://gitlab.se.ifmo.ru/ \
    --registration-token $REGISTRATION_TOKEN_FRONTEND \
    --name "brainstop-runner-1" \
    --tag-list "brainstop-docker" \
    --run-untagged \
    --executor docker \
    --docker-image "ubuntu:24.04" \
    --docker-network-mode "bridge" \
    --docker-pull-policy "if-not-present" \
    --docker-privileged \
    --env "DOCKER_HOST=unix:///var/run/docker.sock" \
    --docker-volumes "/var/run/docker.sock:/var/run/docker.sock" \
```

## Extras

Maybe it will require additional ulimit set:

```sh
    --docker-ulimit "nofile=65536:65536" \
    --docker-ulimit "nproc=65535:65535"
```

## Known issues

Use `sysbox-runc` in engine because or DIND:

```
ERROR: Failed to remove network for build
ERROR: Preparation failed: Error response from daemon: client version 1.25 is too old. Minimum supported API version is 1.44, please upgrade your client to a newer version (docker.go:865:0s)
```
