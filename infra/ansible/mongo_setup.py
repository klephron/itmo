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
    ],
)

HOSTS = ["192.168.122.193", "192.168.122.230", "192.168.122.132"]

DEPLOYMENT_SERVICE_ROUTER = "mongodb"
DEPLOYMENT_SERVICE_CONFIG = "mongodb-config"
DEPLOYMENT_SERVICE_SHARD_PREFIX = "mongodb-shard"

SSH_CMD = "ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o GlobalKnownHostsFile=/dev/null"
SSH_IDENTITY_FILE = os.path.expanduser("~/.ssh/id_ansible")
SSH_USER = "ansible"


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
    return ssh_run(
        args, host, f"docker exec -i {container} {shlex.quote(container_cmd)}"
    )


def config_find(args: Args, host_containers: dict[str, str]):
    for host, containers in host_containers.items():
        for container in containers:
            if container.count(args.deployment_service_config):
                return (host, container)
    return (None, None)


# Only works with single config
def setup_config(args: Args, host_containers: dict[str, str]):
    host, container = config_find(args, host_containers)

    if host is None or container is None:
        raise ValueError("config container is not found")

    result = ssh_run_docker(args, host, container, 'mongosh --eval "rs.initiate()"')

    if result.returncode != 0:
        logging.debug(result.stdout.strip())
        logging.warning("config is probably already initiated")
    else:
        result = ssh_run_docker(
            args,
            host,
            container,
            """mongosh --eval "db.createUser({user: 'admin', pwd: 'admin', roles: [{role: 'root', db: 'admin' }]})" """,
        )

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u admin -p admin --eval "rs.reconfig({{ _id: 'rsc', configsvr: true, members: [ {{ _id: 0, host: '{args.deployment_service_config}:27017', }}, ]}})" """,
    )
    logging.debug(result.stdout)

    result = ssh_run_docker(
        args, host, container, f"""mongosh -u admin -p admin --eval "rs.status()" """
    )
    logging.info(result.stdout)


def setup_shards(args: Args, host_containers: dict[str, str]):
    pass


def add_shards(args: Args, host_containers: dict[str, str]):
    pass


def main(args: Args):
    host_containers = {}

    for host in args.hosts:
        containers = (
            ssh_run(args, host, "docker ps --format '{{.Names}}'")
            .stdout.strip()
            .split()
        )
        host_containers[host] = containers
        logging.info(containers)

    setup_config(args, host_containers)
    setup_shards(args, host_containers)
    add_shards(args, host_containers)


if __name__ == "__main__":
    logging.basicConfig()
    logging.getLogger().setLevel(logging.DEBUG)

    args = Args(
        cwd=os.getcwd(),
        hosts=HOSTS,
        deployment_service_router=DEPLOYMENT_SERVICE_ROUTER,
        deployment_service_config=DEPLOYMENT_SERVICE_CONFIG,
        deployment_service_shard_prefix=DEPLOYMENT_SERVICE_SHARD_PREFIX,
        ssh_cmd=SSH_CMD,
        ssh_identity_file=SSH_IDENTITY_FILE,
        ssh_user=SSH_USER,
    )
    main(args)
