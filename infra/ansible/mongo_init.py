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
    ],
)

HOST = "192.168.122.193"

DEPLOYMENT_SERVICE_ROUTER = "mongodb"

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
    result = ssh_run(
        args, host, f"docker exec -i {container} {shlex.quote(container_cmd)}"
    )

    if result.returncode != 0:
        logging.warning(result.stderr.strip())

    return result


def main(args: Args):
    host_containers = {}

    containers = (
        ssh_run(args, args.host, "docker ps --format '{{.Names}}'")
        .stdout.strip()
        .split()
    )
    host_containers[args.host] = containers

    logging.info(host_containers)


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
    )

    main(args)
