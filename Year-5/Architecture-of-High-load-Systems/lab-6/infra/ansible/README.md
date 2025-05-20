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
ansible-playbook playbooks/setup_client.yml
```

> See ansible role `manager` and `single` `templates/` and `group_vars/all.yml` for further configuration.

## Deployment

### Application

Deploy Docker as single node:

```sh
ansible-playbook playbooks/deploy_single.yml -e infra_root=<infra_root>
```

Deploy Docker Swarm cluster.

First generate mongodb config using script:

```sh
python mongo_config.py <template_stack_file> <template_mongo_dir> <shards_count> <replicas_each_count>
```

> * `template_stack_file` -  use `./roles/deployment_swarm/templates/stacks/docker-compose.app-mongo.yml`
> * `template_mongo_dir` - use `./roles/deployment_swarm/templates/mongodb`
> * `shards_count` - number of shards
> * `replicas_count` - list of number of replicas in each shard
>
> Example:
> ```sh
> python mongo_config.py ./roles/deployment_swarm/templates/stacks/docker-compose.app-mongo.yml ./roles/deployment_swarm/templates/mongodb 2 1 2
> ```
>

And deploy:

```sh
ansible-playbook playbooks/deploy_swarm.yml -e infra_root=<infra_root> -e mongo_keyfile_abspath=<mongo_keyfile_abspath>
```

> * `infra_root` - absolute path to `<repo_root>/infra/`. For example: `-e infra_root="$(realpath $(pwd)/..)"`
> * `mongo_keyfile_abspath` - absolute path to base64 generated keyfile (generated with `openssl rand -base64 756`). For example: `-e mongo_keyfile_abspath="$(realpath $(pwd)/../../examples/mongo-shard-repl-auth/common/keyfile.key)"`

### Application Mongo Cluster

Setup mongo cluster:

```sh
python mongo_setup.py <shards_count> <replicas_each_count>
```

> * `shards_count` - number of shards
> * `replicas_count` - list of number of replicas in each shard

Init mongo cluster:

```sh
python mongo_init.py
```

### Client

Deploy client jenkins agent for testing purposes:

```sh
ansible-playbook playbooks/deploy_client.yml -e '{"jenkins_agent_ssh_pubkey": "<jenkins_agent_ssh_pubkey>"}'
```
