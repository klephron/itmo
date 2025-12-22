# gitlab-runner

Register (e.g. for `brainstop-runner-1`):

```sh
docker exec -it brainstop-runner-1 gitlab-runner register --help
```

```sh
docker exec -it brainstop-runner-1 gitlab-runner register --non-interactive \
    --url https://gitlab.se.ifmo.ru/ \
    --registration-token $REGISTRATION_TOKEN \
    --name "brainstop-runner-1" \
    --run-untagged \
    --executor docker \
    --docker-image "ubuntu:24.04" \
    --docker-network-mode "bridge" \
```
