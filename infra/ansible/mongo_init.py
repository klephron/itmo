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
        "host",
        "deployment_service_router",
        "ssh_cmd",
        "ssh_identity_file",
        "ssh_user",
        "root_username",
        "root_password",
    ],
)

HOST = "192.168.122.193"

DEPLOYMENT_SERVICE_ROUTER = "mongodb"

SSH_CMD = "ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o GlobalKnownHostsFile=/dev/null"
SSH_IDENTITY_FILE = os.path.expanduser("~/.ssh/id_ansible")
SSH_USER = "ansible"

ROOT_USERNAME = "admin"
ROOT_PASSWORD = "admin"


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


def router_find(args: Args, host_containers: dict[str, str]):
    for host, containers in host_containers.items():
        for container in containers:
            if container.count(f"{args.deployment_service_router}."):
                return (host, container)
    return (None, None)


def router_init(args: Args, host_containers: dict[str, str]):
    host, container = router_find(args, host_containers)

    if host is None or container is None:
        logging.error("router is not found")
        return

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'db.getSiblingDB("iot-db").createUser({{user: "zavar", pwd: "zavar", roles: [{{role: "readWrite", db: "iot-db"}}]}})'""",
    )
    logging.info(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'sh.enableSharding("iot-db")'""",
    )
    logging.info(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'db.getSiblingDB("iot-db").createCollection("bills")'""",
    )
    logging.info(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'db.getSiblingDB("iot-db").bills.createIndex({{ userId: 1 }})'""",
    )
    logging.info(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'sh.shardCollection("iot-db.bills", {{ userId: 1 }})'""",
    )
    logging.info(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'db.getSiblingDB("iot-db").getUsers()'""",
    )
    logging.info(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'db.settings.updateMany({{ _id: "chunksize" }}, {{ $set: {{ value: 1 }} }}, {{ upsert: true }})'""",
    )
    logging.info(result.stdout.strip())

    result = ssh_run_docker(
        args,
        host,
        container,
        f"""mongosh -u '{args.root_username}' -p '{args.root_password}' 'admin' --eval 'sh.status()'""",
    )
    logging.info(result.stdout.strip())


def main(args: Args):
    host_containers = {}

    containers = (
        ssh_run(args, args.host, "docker ps --format '{{.Names}}'")
        .stdout.strip()
        .split()
    )
    host_containers[args.host] = containers

    logging.info(host_containers)

    router_init(args, host_containers)


if __name__ == "__main__":
    logging.basicConfig()
    logging.getLogger().setLevel(logging.DEBUG)

    args = Args(
        cwd=os.getcwd(),
        host=HOST,
        deployment_service_router=DEPLOYMENT_SERVICE_ROUTER,
        ssh_cmd=SSH_CMD,
        ssh_identity_file=SSH_IDENTITY_FILE,
        ssh_user=SSH_USER,
        root_username=ROOT_USERNAME,
        root_password=ROOT_PASSWORD,
    )

    main(args)
