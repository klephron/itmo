import os
import sys
import subprocess
from collections import namedtuple
import logging
import shlex

Args = namedtuple(
    "Args",
    [
        "cwd",
        "hosts",
        "deployment_service_router",
        "deployment_service_config",
        "deployment_service_shard_prefix",
        "ssh_cmd",
        "ssh_identity_file",
        "ssh_user",
        "shards_count",
        "replicas_each_count",
    ],
)

HOSTS = ["192.168.122.193", "192.168.122.230", "192.168.122.132"]

DEPLOYMENT_SERVICE_ROUTER = "mongodb"
DEPLOYMENT_SERVICE_CONFIG = "mongodb-config"
DEPLOYMENT_SERVICE_SHARD_PREFIX = "mongodb-shard"

SSH_CMD = "ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o GlobalKnownHostsFile=/dev/null"
SSH_IDENTITY_FILE = os.path.expanduser("~/.ssh/id_ansible")
SSH_USER = "ansible"

SHARDS_COUNT = 2
REPLICAS_EACH_COUNT = [2, 2]  # for each shard count of replicas


def ssh_get_cmd(args: Args, host: str):
    return f"{args.ssh_cmd} -i {args.ssh_identity_file} {args.ssh_user}@{host}"


def ssh_run(args: Args, host: str, cmd: str):
    ssh = ssh_get_cmd(args, host)

    ssh_cmd = f"{ssh} {cmd}"
    logging.debug(ssh_cmd)

    return subprocess.run(
        ssh_cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        shell=True,
        text=True,
    )


def ssh_run_docker(args: Args, host: str, container: str, container_cmd: str):
    result = ssh_run(
        args, host, f"docker exec -i {container} {shlex.quote(container_cmd)}"
    )

    if result.returncode != 0:
        logging.warning(result.stderr.strip())

    return result


def config_find(args: Args, host_containers: dict[str, str]):
    for host, containers in host_containers.items():
        for container in containers:
            if container.count(f"{args.deployment_service_config}."):
                return (host, container)
    return (None, None)


# Only works with single config
def setup_config(args: Args, host_containers: dict[str, str]):
    host, container = config_find(args, host_containers)

    if host is None or container is None:
        raise ValueError("config container is not found")

    result = ssh_run_docker(args, host, container, """mongosh --eval 'rs.initiate()'""")

    if result.returncode != 0:
        logging.debug(result.stdout.strip())
        logging.warning("config is probably already initiated")
    else:
        result = ssh_run_docker(
            args,
            host,
            container,
            """mongosh admin --eval 'db.createUser({user: "admin", pwd: "admin", roles: [{role: "root", db: "admin" }]})'""",
        )
        logging.debug(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u admin -p admin --eval 'rs.reconfig({{ _id: "rsc", configsvr: true, members: [ {{ _id: 0, host: "{args.deployment_service_config}:27017", }}, ]}})'""",
    )
    logging.debug(result.stdout.strip())

    result = ssh_run_docker(
        args, host, container, f"""mongosh -u admin -p admin --eval 'rs.status()'"""
    )
    logging.info(result.stdout.strip())


def shard_find(
    args: Args, host_containers: dict[str, str], shard_i: int, replica_i: int
):
    for host, containers in host_containers.items():
        for container in containers:
            if container.count(
                f"{args.deployment_service_shard_prefix}-{shard_i}-{replica_i}."
            ):
                return (host, container)
    return (None, None)


def setup_shard(
    args: Args, host_containers: dict[str, str], shard_i: int, replica_i: int
):
    host, container = shard_find(args, host_containers, shard_i, replica_i)
    if host is None or container is None:
        logging.warning(f"shard={shard_i}, replica={replica_i} is probably not running")
        return

    if replica_i != 1:
        return

    result = ssh_run_docker(args, host, container, """mongosh --eval 'rs.initiate()'""")

    if result.returncode != 0:
        logging.debug(result.stdout.strip())
        logging.warning("config is probably already initiated")
    else:
        result = ssh_run_docker(
            args,
            host,
            container,
            """mongosh admin --eval 'db.createUser({user: "shard-admin", pwd: "shard-admin", roles: [{role: "root", db: "admin" }]})'""",
        )
        logging.debug(result.stdout.strip())

    members = ",".join(
        [
            f"""{{ _id: {i}, host: "{args.deployment_service_shard_prefix}-{shard_i}-{i + 1}:27017" }}"""
            for i in range(args.replicas_each_count[shard_i - 1])
        ]
    )

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u shard-admin -p shard-admin --eval 'rs.reconfig({{ _id: "rsh-{shard_i}", members: [{members}]}})'""",
    )
    logging.debug(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u shard-admin -p shard-admin --eval 'rs.status()'""",
    )
    logging.info(result.stdout.strip())


def setup_shards(args: Args, host_containers: dict[str, str]):
    for i in range(args.shards_count):
        shard_i = i + 1
        replicas = args.replicas_each_count[i]
        for j in range(replicas):
            replica_i = j + 1
            setup_shard(args, host_containers, shard_i, replica_i)


def router_find(args: Args, host_containers: dict[str, str]):
    for host, containers in host_containers.items():
        for container in containers:
            if container.count(f"{args.deployment_service_router}."):
                return (host, container)
    return (None, None)


def add_shards(args: Args, host_containers: dict[str, str]):
    host, container = router_find(args, host_containers)

    if host is None or container is None:
        logging.warning(f"router is probably not running")
        return

    for i in range(args.shards_count):
        shard_i = i + 1

        result = ssh_run_docker(
            args,
            host,
            container,
            f"""mongosh -u admin -p admin --eval 'sh.addShard("rsh-{shard_i}/{args.deployment_service_shard_prefix}-{shard_i}-1:27017")'""",
        )
        logging.debug(result.stdout.strip())

    result = ssh_run_docker(
        args, host, container, f"""mongosh -u admin -p admin --eval 'sh.status()'"""
    )
    logging.info(result.stdout.strip())


def main(args: Args):
    host_containers = {}

    for host in args.hosts:
        containers = (
            ssh_run(args, host, "docker ps --format '{{.Names}}'")
            .stdout.strip()
            .split()
        )
        host_containers[host] = containers

    logging.info(host_containers)

    setup_config(args, host_containers)
    setup_shards(args, host_containers)
    add_shards(args, host_containers)


if __name__ == "__main__":
    logging.basicConfig()
    logging.getLogger().setLevel(logging.DEBUG)

    assert len(REPLICAS_EACH_COUNT) == SHARDS_COUNT

    if len(sys.argv) < 2:
        raise ValueError("invalid arguments")

    shards_count = int(sys.argv[1])

    if len(sys.argv) != 2 + shards_count:
        raise ValueError("invalid arguments")

    replicas_each_count = [int(r) for r in sys.argv[2:]]

    args = Args(
        cwd=os.getcwd(),
        hosts=HOSTS,
        deployment_service_router=DEPLOYMENT_SERVICE_ROUTER,
        deployment_service_config=DEPLOYMENT_SERVICE_CONFIG,
        deployment_service_shard_prefix=DEPLOYMENT_SERVICE_SHARD_PREFIX,
        ssh_cmd=SSH_CMD,
        ssh_identity_file=SSH_IDENTITY_FILE,
        ssh_user=SSH_USER,
        shards_count=shards_count,
        replicas_each_count=replicas_each_count,
    )
    main(args)
