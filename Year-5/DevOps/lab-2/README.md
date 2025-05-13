# DevOpsLabs

## Build

Build and run:

```bash
docker-compose up --build
```

## Ansible

Gather facts (example):

```bash
ansible all -m setup --user ubuntu --ask-pass
```

Bootstrap:

```bash
ansible-playbook bootstrap.yml --user ubuntu --ask-pass
```

> Password for user `ubuntu` is configured in terraform `cloud_init.cfg`.

Install docker:

```bash
ansible-playbook docker.yml
```

Install compose services:

```bash
ansible-playbook docker-compose_up.yml
```

## Services

### Docker compose

Server Swagger URL:

```bash
http://localhost:8080/swagger-ui/index.html
```

Front URL:

```bash
http://localhost:8081/index.html
```

Jenkins Controller URL:

```bash
http://localhost:8082/
```
