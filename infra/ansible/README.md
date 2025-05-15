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

Deploy Docker Swarm cluster:

```sh
ansible-playbook playbooks/deploy_swarm.yml -e infra_root=<infra_root> -e mongo_keyfile_abspath=<mongo_keyfile_abspath>
```

Or deploy Docker as single node:

```sh
ansible-playbook playbooks/deploy_single.yml -e infra_root=<infra_root>
```

> * `infra_root` - absolute path to `<repo_root>/infra/`. For example: `-e infra_root="$(realpath $(pwd)/..)"`
> * `mongo_keyfile_abspath` - absolute path to base64 generated keyfile (generated with `openssl rand -base64 756`). For example: `-e mongo_keyfile_abspath="$(realpath $(pwd)/../../examples/mongo-shard-repl-auth/common/keyfile.key)"`


### Application Mongo Cluster Configuration

After mongo cluster is deployed currently it need to be configured manually.

1. Open bash console in `mongodb-config`.

```sh
mongosh
```

2. Run commands one by one:

```js
rs.initiate()
// rs.status()

use admin
db.createUser({user: 'admin', pwd: 'admin', roles: [{role: 'root', db: 'admin' }]})
```

3. Open bash console for each mongo shard primary node (ex. `mongodb-shard-1-1`, `mongodb-shard-2-1`, etc.):

```sh
mongosh
```

4. For each primary node run commands one by one (ex. for shard `rsh-1` primary):

```js
rs.initiate() // initiate primary node

use admin
db.createUser({user: 'shard-admin', pwd: 'shard-admin', roles: [{role: 'root', db: 'admin' }]})
db.auth('shard-admin', 'shard-admin')
// db.runCommand({ connectionStatus: 1 })

rs.reconfig({
  _id: "rsh-1",
  members: [
    {
      _id: 0,
      host: "mongodb-shard-1-1:27017",
    },
    {
      _id: 1,
      host: "mongodb-shard-1-2:27017",
    }
  ]
})
// rs.status()
```

> `_id` - shard name in config.

5. Open console in `mongodb` (mongodb router).

```sh
mongosh -u admin -p admin
```

> User and password should match user and password on `mongodb-config`.


6. Run commands one by one:

```js
use admin
sh.addShard( "rsh-1/mongodb-shard-1-1:27017")
...
```

> Add all configured shards.

7. Create database, collection, users, enable sharding:

```js
use admin

db.getSiblingDB("iot-db");

db.createUser({
    user: "zavar",
    pwd: "zavar",
    roles: [{ role: "readWrite", db: "iot-db" }],
});

use iot-db
db.createCollection("data")
db.data.createIndex({ userId: 1 })

sh.shardCollection("iot-db.data", { userId: 1 })
```

### Client

Deploy client jenkins agent for testing purposes:

```sh
ansible-playbook playbooks/deploy_client.yml -e '{"jenkins_agent_ssh_pubkey": "<jenkins_agent_ssh_pubkey>"}'
```
