# Ansible

> Recommended to use ansible `dockerfile` with network `host` to run playbooks.

## Bootstrap

```sh
ansible-playbook playbooks/bootstrap.yml --user ubuntu --ask-pass
```

> Default password is `ubuntu`. Can be configured in `cloudinit` in `terraform` `ubuntu` module.

See `ansible.cfg` `private_key_file` and specify `bootstrap_ssh_pubkey` to use different token.

## Node Setup

Setup Docker Swarm cluster:

```sh
ansible-playbook playbooks/setup_swarm.yml
```

Or setup Docker as single node:

```sh
ansible-playbook playbooks/setup_single.yml
```

Setup testing environment:

```sh
ansible-playbook playbooks/setup_client.yml -e '{"jenkins_agent_ssh_pubkey": "<jenkins_agent_ssh_pubkey>"}'
```

> See ansible role `manager` and `single` `templates/` and `group_vars/all.yml` for further configuration.

## Deployment

Deploy Docker Swarm cluster:

```sh
ansible-playbook playbooks/deploy_swarm.yml
```

Or deploy Docker as single node:

```sh
ansible-playbook playbooks/deploy_single.yml
```
