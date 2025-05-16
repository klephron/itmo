import os
import sys
from collections import namedtuple
import logging

Args = namedtuple(
    "Args",
    [
        "cwd",
        "dir_path",
        "common_subdir",
        "config_subdir",
        "router_subdir",
        "shards_subdir",
        "shards_count",
        "replicas_each_count",
        "deployment_mongo_image",
        "deployment_service_router",
        "deployment_service_config",
        "deployment_service_shard_prefix",
        "deployment_config_path",
        "deployment_stack_file",
    ],
)

COMMON_SUBDIR = "common"
CONFIG_SUBDIR = "config"
ROUTER_SUBDIR = "router"
SHARDS_SUBDIR = "shards"

SHARDS_COUNT = 2
REPLICAS_EACH_COUNT = [2, 2]  # for each shard count of replicas

DEPLOYMENT_MONGO_IMAGE = "mongo:8.0"
DEPLOYMENT_SERVICE_ROUTER = "mongodb"
DEPLOYMENT_SERVICE_CONFIG = "mongodb-config"
DEPLOYMENT_SERVICE_SHARD_PREFIX = "mongodb-shard"
DEPLOYMENT_CONFIG_PATH = "{{ deployment_swarm_src }}/mongodb"


def get_dir(args):
    return os.path.join(args.dir_path)


def get_common_dir(args: Args):
    return os.path.join(get_dir(args), args.common_subdir)


def get_config_dir(args: Args):
    return os.path.join(get_dir(args), args.config_subdir)


def get_router_dir(args: Args):
    return os.path.join(get_dir(args), args.router_subdir)


def get_shards_dir(args: Args):
    return os.path.join(get_dir(args), args.shards_subdir)


def gen_common(args: Args):
    os.makedirs(get_common_dir(args), exist_ok=True)

    with open(os.path.join(get_common_dir(args), "keyfile_install.sh"), "w") as file:
        file.write(
            """
#!/bin/bash

cp /etc/mongo-common/keyfile.key /etc/mongo.key
chmod 400 /etc/mongo.key
chown 999:999 /etc/mongo.key
                   """.strip()
        )

    with open(os.path.join(get_common_dir(args), "trap.sh"), "w") as file:
        file.write(
            """
#!/bin/bash

handle_sigterm() {
  echo "Received SIGTERM. Performing graceful shutdown..."

  if [ -n "$MAIN_PID" ]; then
    kill -TERM "$MAIN_PID" 2>/dev/null
  fi

  exit 0
}

trap handle_sigterm SIGTERM
                """.strip()
        )


def gen_config(args: Args):
    os.makedirs(get_config_dir(args), exist_ok=True)

    with open(os.path.join(get_config_dir(args), "entrypoint.sh"), "w") as file:
        file.write(
            """
source /etc/mongo-common/trap.sh
source /etc/mongo-common/keyfile_install.sh

mkdir -p /var/lib/mongo/db

mongod $@ &

MAIN_PID=$!
wait $MAIN_PID
                   """.strip()
        )

    with open(os.path.join(get_config_dir(args), "mongod.conf"), "w") as file:
        file.write(
            """
storage:
  dbPath: /var/lib/mongo/db

security:
  authorization: disabled
  keyFile: /etc/mongo.key

net:
  port: 27017
  bindIpAll: true

sharding:
  clusterRole: configsvr

replication:
  replSetName: rsc
                   """.strip()
        )


def gen_router(args: Args):
    os.makedirs(get_router_dir(args), exist_ok=True)

    with open(os.path.join(get_router_dir(args), "entrypoint.sh"), "w") as file:
        file.write(
            """
source /etc/mongo-common/trap.sh
source /etc/mongo-common/keyfile_install.sh

mongos $@ &

MAIN_PID=$!
wait $MAIN_PID
                   """.strip()
        )

    with open(os.path.join(get_router_dir(args), "mongos.conf"), "w") as file:
        file.write(
            f"""
security:
  keyFile: /etc/mongo.key

net:
  port: 27017
  bindIpAll: true

sharding:
  configDB: rsc/{args.deployment_service_config}:27017
                   """.strip()
        )


def gen_shards(args: Args):
    os.makedirs(get_shards_dir(args), exist_ok=True)

    with open(os.path.join(get_shards_dir(args), "entrypoint.sh"), "w") as file:
        file.write(
            """
source /etc/mongo-common/trap.sh
source /etc/mongo-common/keyfile_install.sh

mkdir -p /var/lib/mongo/db

mongod $@ &

MAIN_PID=$!
wait $MAIN_PID
                   """.strip()
        )

    for i in range(args.shards_count):
        shard_i = i + 1
        replicas = args.replicas_each_count[i]

        for j in range(replicas):
            replica_i = j + 1
            with open(
                os.path.join(
                    get_shards_dir(args), f"mongod-{shard_i}-{replica_i}.conf"
                ),
                "w",
            ) as file:
                file.write(
                    f"""
storage:
  dbPath: /var/lib/mongo

security:
  authorization: disabled
  keyFile: /etc/mongo.key

net:
  bindIp: 0.0.0.0
  port: 27017

sharding:
  clusterRole: shardsvr

replication:
  replSetName: rsh-{shard_i}
                           """.strip()
                )


def gen_stack_file(args: Args):
    content = f"""services:
  {args.deployment_service_router}:
    image: {args.deployment_mongo_image}
    volumes:
      - "{args.deployment_config_path}/common:/etc/mongo-common"
      - "{args.deployment_config_path}/router:/etc/mongo"
      - mongodb-data:/var/lib/mongo
    command: --config /etc/mongo/mongos.conf
    entrypoint: ["bash", "/etc/mongo/entrypoint.sh"]
    deploy:
      mode: global
      placement:
        constraints:
          - node.role == manager
    networks:
      - ahla-net

  {args.deployment_service_config}:
    image: {args.deployment_mongo_image}
    volumes:
      - "{args.deployment_config_path}/common:/etc/mongo-common"
      - "{args.deployment_config_path}/config:/etc/mongo"
      - mongodb-config-data:/var/lib/mongo
    command: --config /etc/mongo/mongod.conf
    entrypoint: ["bash", "/etc/mongo/entrypoint.sh"]
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == worker
    networks:
      - ahla-net
                   """

    for i in range(args.shards_count):
        shard_i = i + 1
        replicas = args.replicas_each_count[i]

        for j in range(replicas):
            replica_i = j + 1

            content += f"""
  mongodb-shard-{shard_i}-{replica_i}:
    image: mongo:8.0
    volumes:
      - "{args.deployment_config_path}/common:/etc/mongo-common"
      - "{args.deployment_config_path}/shards:/etc/mongo"
      - mongodb-shard-{shard_i}-{replica_i}-data:/var/lib/mongo
    command: --config /etc/mongo/mongod-{shard_i}-{replica_i}.conf
    entrypoint: ["bash", "/etc/mongo/entrypoint.sh"]
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == worker
    networks:
      - ahla-net
                   """

    content += """
networks:
  ahla-net:
    external: true
    """

    content += """
volumes:
  mongodb-data:
  mongodb-config-data:
"""

    for i in range(args.shards_count):
        shard_i = i + 1
        replicas = args.replicas_each_count[i]

        for j in range(replicas):
            replica_i = j + 1

            content += f"""  mongodb-shard-{shard_i}-{replica_i}-data:
"""

    with open(args.deployment_stack_file, "w") as file:
        file.write(content)


def main(args: Args):
    config_dir = os.path.join(args.dir_path)

    if os.path.isdir(config_dir):
        logging.warning(f"{config_dir} already exists, may be overwritten")

    gen_common(args)
    gen_config(args)
    gen_router(args)
    gen_shards(args)
    gen_stack_file(args)


if __name__ == "__main__":
    assert len(REPLICAS_EACH_COUNT) == SHARDS_COUNT

    if len(sys.argv) < 4:
        raise ValueError("invalid arguments, see main")

    deployment_stack_file = sys.argv[1]
    dir_path = sys.argv[2]
    shards_count = int(sys.argv[3])

    if len(sys.argv) != 4 + shards_count:
        raise ValueError("invalid arguments, see main")

    replicas_each_count = [int(r) for r in sys.argv[4:]]

    args = Args(
        cwd=os.getcwd(),
        dir_path=dir_path,
        common_subdir=COMMON_SUBDIR,
        config_subdir=CONFIG_SUBDIR,
        router_subdir=ROUTER_SUBDIR,
        shards_subdir=SHARDS_SUBDIR,
        shards_count=shards_count,
        replicas_each_count=replicas_each_count,
        deployment_mongo_image=DEPLOYMENT_MONGO_IMAGE,
        deployment_service_router=DEPLOYMENT_SERVICE_ROUTER,
        deployment_service_config=DEPLOYMENT_SERVICE_CONFIG,
        deployment_service_shard_prefix=DEPLOYMENT_SERVICE_SHARD_PREFIX,
        deployment_config_path=DEPLOYMENT_CONFIG_PATH,
        deployment_stack_file=deployment_stack_file,
    )
    main(args)
