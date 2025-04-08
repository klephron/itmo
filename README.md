# DevOpsLabs

## Build

Build and run:

```bash
docker-compose up --build
```

## Ansible

Gather facts (example):

```bash
ansible all -i inventory/hosts.ini -m setup -u zubrailx --ask-pass
```

Bootstrap:

```bash
ansible-playbook -i inventory/hosts.ini bootstrap.yml --user=zubrailx --ask-pass --ask-become-pass
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
